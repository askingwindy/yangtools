/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang2sources.plugin;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.opendaylight.yangtools.yang.parser.util.NamedFileInputStream;
import org.opendaylight.yangtools.yang2sources.plugin.ConfigArg.CodeGeneratorArg;
import org.opendaylight.yangtools.yang2sources.plugin.Util.ContextHolder;
import org.opendaylight.yangtools.yang2sources.plugin.Util.YangsInZipsResult;
import org.opendaylight.yangtools.yang2sources.spi.BuildContextAware;
import org.opendaylight.yangtools.yang2sources.spi.CodeGenerator;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

class YangToSourcesProcessor {
    static final String LOG_PREFIX = "yang-to-sources:";
    static final String META_INF_YANG_STRING = "META-INF" + File.separator + "yang";
    static final String META_INF_YANG_STRING_JAR = "META-INF" + "/" + "yang";
    static final File META_INF_YANG_DIR = new File(META_INF_YANG_STRING);

    private final Log log;
    private final File yangFilesRootDir;
    private final File[] excludedFiles;
    private final List<CodeGeneratorArg> codeGenerators;
    private final MavenProject project;
    private final boolean inspectDependencies;
    private final BuildContext buildContext;
    private YangProvider yangProvider;

    @VisibleForTesting
    YangToSourcesProcessor(Log log, File yangFilesRootDir, File[] excludedFiles, List<CodeGeneratorArg> codeGenerators,
            MavenProject project, boolean inspectDependencies, YangProvider yangProvider) {
        this(new DefaultBuildContext(), log, yangFilesRootDir, excludedFiles, codeGenerators, project, inspectDependencies, yangProvider);
    }

    private YangToSourcesProcessor(BuildContext buildContext,  Log log, File yangFilesRootDir, File[] excludedFiles,
            List<CodeGeneratorArg> codeGenerators, MavenProject project, boolean inspectDependencies, YangProvider yangProvider) {
        this.buildContext = Util.checkNotNull(buildContext, "buildContext");
        this.log = Util.checkNotNull(log, "log");
        this.yangFilesRootDir = Util.checkNotNull(yangFilesRootDir, "yangFilesRootDir");
        this.excludedFiles = new File[excludedFiles.length];
        int i = 0;
        for (File file : excludedFiles) {
            this.excludedFiles[i++] = new File(file.getPath());
        }
        this.codeGenerators = Collections.unmodifiableList(Util.checkNotNull(codeGenerators, "codeGenerators"));
        this.project = Util.checkNotNull(project, "project");
        this.inspectDependencies = inspectDependencies;
        this.yangProvider = yangProvider;
    }

    YangToSourcesProcessor(BuildContext buildContext, Log log, File yangFilesRootDir, File[] excludedFiles, List<CodeGeneratorArg> codeGenerators,
            MavenProject project, boolean inspectDependencies) {
        this(log, yangFilesRootDir, excludedFiles, codeGenerators, project, inspectDependencies, new YangProvider());
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        ContextHolder context = processYang();
        if (context != null) {
            generateSources(context);
            yangProvider.addYangsToMetaInf(log, project, yangFilesRootDir, excludedFiles);
        }
    }

    private ContextHolder processYang() throws MojoExecutionException {
        YangParserImpl parser = new YangParserImpl();
        List<Closeable> closeables = new ArrayList<>();
        log.info(Util.message("Inspecting %s", LOG_PREFIX, yangFilesRootDir));
        try {
            /*
             * Collect all files which affect YANG context. This includes all
             * files in current project and optionally any jars/files in the
             * dependencies.
             */
            final Collection<File> yangFilesInProject = Util.listFiles(yangFilesRootDir, excludedFiles, log);
            final Collection<File> allFiles = new ArrayList<>(yangFilesInProject);
            if (inspectDependencies) {
                allFiles.addAll(Util.findYangFilesInDependencies(log, project));
            }

            if (allFiles.isEmpty()) {
            	log.info(Util.message("No input files found", LOG_PREFIX));
            	return null;
            }

            /*
             * Check if any of the listed files changed. If no changes occurred,
             * simply return null, which indicates and of execution.
             */
            boolean noChange = true;
            for (final File f : allFiles) {
                if (buildContext.hasDelta(f)) {
                    log.debug(Util.message("buildContext %s indicates %s changed, forcing regeneration", LOG_PREFIX, buildContext, f));
                    noChange = false;
                }
            }

            if (noChange) {
            	log.info(Util.message("None of %s input files changed", LOG_PREFIX, allFiles.size()));
                return null;
            }

            final List<InputStream> yangsInProject = new ArrayList<>();
            for (final File f : yangFilesInProject) {
                yangsInProject.add(new NamedFileInputStream(f, META_INF_YANG_STRING + File.separator + f.getName()));
            }

            List<InputStream> all = new ArrayList<>(yangsInProject);
            closeables.addAll(yangsInProject);
            Map<InputStream, Module> allYangModules;
            Set<Module> projectYangModules;
            try {
                if (inspectDependencies) {
                    YangsInZipsResult dependentYangResult = Util.findYangFilesInDependenciesAsStream(log, project);
                    Closeable dependentYangResult1 = dependentYangResult;
                    closeables.add(dependentYangResult1);
                    all.addAll(dependentYangResult.getYangStreams());
                }

                allYangModules = parser.parseYangModelsFromStreamsMapped(all);

                projectYangModules = new HashSet<>();
                for (InputStream inProject : yangsInProject) {
                    Module module = checkNotNull(allYangModules.get(inProject), "Cannot find module by %s", inProject);
                    projectYangModules.add(module);
                }

            } finally {
                for (AutoCloseable closeable : closeables) {
                    closeable.close();
                }
            }

            Set<Module> parsedAllYangModules = new HashSet<>(allYangModules.values());
            SchemaContext resolveSchemaContext = parser.resolveSchemaContext(parsedAllYangModules);
            log.info(Util.message("%s files parsed from %s", LOG_PREFIX, Util.YANG_SUFFIX.toUpperCase(), yangsInProject));
            return new ContextHolder(resolveSchemaContext, projectYangModules);

            // MojoExecutionException is thrown since execution cannot continue
        } catch (Exception e) {
            String message = Util.message("Unable to parse %s files from %s", LOG_PREFIX, Util.YANG_SUFFIX,
                    yangFilesRootDir);
            log.error(message, e);
            throw new MojoExecutionException(message, e);
        }
    }

    static class YangProvider {

        private static final String YANG_RESOURCE_DIR = "target" + File.separator + "yang";

        void addYangsToMetaInf(Log log, MavenProject project, File yangFilesRootDir, File[] excludedFiles)
                throws MojoFailureException {
            File targetYangDir = new File(project.getBasedir(), YANG_RESOURCE_DIR);

            try {
                Collection<File> files = Util.listFiles(yangFilesRootDir, excludedFiles, null);
                for (File file : files) {
                    org.apache.commons.io.FileUtils.copyFile(file, new File(targetYangDir, file.getName()));
                }
            } catch (IOException e) {
                String message = "Unable to list yang files into resource folder";
                log.warn(message, e);
                throw new MojoFailureException(message, e);
            }

            setResource(targetYangDir, META_INF_YANG_STRING_JAR, project);

            log.debug(Util.message("Yang files from: %s marked as resources: %s", LOG_PREFIX, yangFilesRootDir,
                    META_INF_YANG_STRING_JAR));
        }

        private static void setResource(File targetYangDir, String targetPath, MavenProject project) {
            Resource res = new Resource();
            res.setDirectory(targetYangDir.getPath());
            if (targetPath != null) {
                res.setTargetPath(targetPath);
            }
            project.addResource(res);
        }
    }

    /**
     * Call generate on every generator from plugin configuration
     */
    private void generateSources(ContextHolder context) throws MojoFailureException {
        if (codeGenerators.size() == 0) {
            log.warn(Util.message("No code generators provided", LOG_PREFIX));
            return;
        }

        Map<String, String> thrown = Maps.newHashMap();
        for (CodeGeneratorArg codeGenerator : codeGenerators) {
            try {
                generateSourcesWithOneGenerator(context, codeGenerator);
            } catch (Exception e) {
                // try other generators, exception will be thrown after
                log.error(
                        Util.message("Unable to generate sources with %s generator", LOG_PREFIX,
                                codeGenerator.getCodeGeneratorClass()), e);
                thrown.put(codeGenerator.getCodeGeneratorClass(), e.getClass().getCanonicalName());
            }
        }

        if (!thrown.isEmpty()) {
            String message = Util.message(
                    "One or more code generators failed, including failed list(generatorClass=exception) %s",
                    LOG_PREFIX, thrown.toString());
            log.error(message);
            throw new MojoFailureException(message);
        }
    }

    /**
     * Instantiate generator from class and call required method
     */
    private void generateSourcesWithOneGenerator(ContextHolder context, CodeGeneratorArg codeGeneratorCfg)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {

        codeGeneratorCfg.check();

        CodeGenerator g = Util.getInstance(codeGeneratorCfg.getCodeGeneratorClass(), CodeGenerator.class);
        log.info(Util.message("Code generator instantiated from %s", LOG_PREFIX,
                codeGeneratorCfg.getCodeGeneratorClass()));

        File outputDir = codeGeneratorCfg.getOutputBaseDir(project);

        log.info(Util.message("Sources will be generated to %s", LOG_PREFIX, outputDir));
        log.debug(Util.message("Project root dir is %s", LOG_PREFIX, project.getBasedir()));
        log.debug(Util.message("Additional configuration picked up for : %s: %s", LOG_PREFIX,
                codeGeneratorCfg.getCodeGeneratorClass(), codeGeneratorCfg.getAdditionalConfiguration()));

        if (outputDir != null) {
            project.addCompileSourceRoot(outputDir.getAbsolutePath());
        }

        if (g instanceof BuildContextAware) {
            ((BuildContextAware)g).setBuildContext(buildContext);
        }
        g.setLog(log);
        g.setMavenProject(project);
        g.setAdditionalConfig(codeGeneratorCfg.getAdditionalConfiguration());
        File resourceBaseDir = codeGeneratorCfg.getResourceBaseDir(project);

        YangProvider.setResource(resourceBaseDir, null, project);
        g.setResourceBaseDir(resourceBaseDir);
        log.debug(Util.message("Folder: %s marked as resources for generator: %s", LOG_PREFIX, resourceBaseDir,
                codeGeneratorCfg.getCodeGeneratorClass()));

        Collection<File> generated = g.generateSources(context.getContext(), outputDir, context.getYangModules());

        log.info(Util.message("Sources generated by %s: %s", LOG_PREFIX, codeGeneratorCfg.getCodeGeneratorClass(),
                generated));
    }

}
