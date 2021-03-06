/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.*;

import java.net.URI;
import java.util.Map;

/**
 * @author michal.rehak
 * @param <T>
 *            type of node value
 *
 */
public abstract class AbstractNodeTO<T> implements Node<T>, NodeModification {

    private QName qName;
    private CompositeNode parent;
    private T value;
    private ModifyAction modifyAction;

    // Only for Serialization use
    public AbstractNodeTO(){

    }

    /**
     * @param qname
     * @param parent
     * @param value
     */
    public AbstractNodeTO(QName qname, CompositeNode parent, T value) {
        this(qname, parent, value, null);
    }

    /**
     * @param qname
     * @param parent
     * @param value
     * @param modifyAction
     */
    public AbstractNodeTO(QName qname, CompositeNode parent, T value, ModifyAction modifyAction) {
        this.qName = qname;
        this.parent = parent;
        this.value = value;
        this.modifyAction = modifyAction;
    }

    @Override
    public QName getNodeType() {
        return qName;
    }

    /**
     * @return the qName
     */
    public QName getQName() {
        return qName;
    }

    @Override
    public CompositeNode getParent() {
        return parent;
    }

    /**
     * @param parent
     *            the parent to set
     */
    public void setParent(CompositeNode parent) {
        this.parent = parent;
    }

    /**
     * @param value
     *            the value to set
     */
    public T setValue(T value) {
        T oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    @Override
    public T getValue() {
        return value;
    }

    /**
     * @return modification action
     * @see NodeModification#getModificationAction()
     */
    @Override
    public ModifyAction getModificationAction() {
        return modifyAction;
    }

    /**
     * @param modifyAction
     *            the modifyAction to set
     */
    protected void setModificationAction(ModifyAction modifyAction) {
        this.modifyAction = modifyAction;
    }

    @Override
    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append(String.format("Node[%s], qName[%s], modify[%s]", getClass().getSimpleName(), getQName()
                .getLocalName(), getModificationAction() == null ? "n/a" : getModificationAction()));
        return out.toString();
    }


    @Override
    public final QName getKey() {
        return getNodeType();
    }

    /* */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((qName == null) ? 0 : qName.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result % 2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        AbstractNodeTO<T> other = (AbstractNodeTO<T>) obj;
        if (parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (other.parent == null) {
            return false;
        }
        if (qName == null) {
            if (other.qName != null) {
                return false;
            }
        } else if (!qName.equals(other.qName)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
    /* */


    //Serialization related

    protected final void init(QName qName, CompositeNode parent, T value, ModifyAction modifyAction){
        this.qName = qName;
        this.modifyAction = modifyAction;
        this.parent = parent;
        this.value = value;
    }
}
