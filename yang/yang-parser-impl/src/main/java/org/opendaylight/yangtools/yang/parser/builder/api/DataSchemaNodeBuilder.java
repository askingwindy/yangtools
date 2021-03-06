/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.builder.api;

import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.parser.builder.impl.ConstraintsBuilder;

/**
 * Interface for all yang data-schema nodes [anyxml, case, container, grouping,
 * list, module, notification].
 */
public interface DataSchemaNodeBuilder extends SchemaNodeBuilder, GroupingMember {

    /**
     * Build DataSchemaNode object from this builder.
     */
    DataSchemaNode build();

    void setPath(SchemaPath path);

    /**
     *
     * @return true, if this node is added by augmentation, false otherwise
     */
    boolean isAugmenting();

    /**
     * Set if this node is added by augmentation.
     *
     * @param augmenting
     */
    void setAugmenting(boolean augmenting);

    /**
     * Get value of config statement.
     *
     * @return value of config statement
     */
    boolean isConfiguration();

    /**
     * Set config statement.
     *
     * @param config
     */
    void setConfiguration(boolean config);

    /**
     * Get constraints of this builder.
     *
     * @return constraints of this builder
     */
    ConstraintsBuilder getConstraints();

}
