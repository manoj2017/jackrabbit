/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;

import java.io.Serializable;

/**
 * <code>ItemInfoImpl</code> is a base class for <code>ItemInfo</code>
 * implementations.
 */
public abstract class ItemInfoImpl implements ItemInfo, Serializable {

    /**
     * The parent node id of this item or <code>null</code> if this item
     * represents the root node info.
     */
    private final NodeId parentId;

    /**
     * The name of this item info.
     */
    private final QName name;

    /**
     * The path of this item info.
     */
    private final Path path;

    /**
     * Flag indicating whether this is a node or a property info.
     */
    private final boolean isNode;

    /**
     * Creates a new serializable item info for the given qualified
     * <code>item</code> info.
     *
     * @param parentId the parent id.
     * @param name     the name of this item.
     * @param path     the path to this item.
     * @param isNode   if this item is a node.
     */
    public ItemInfoImpl(NodeId parentId, QName name, Path path, boolean isNode) {
        this.parentId = parentId;
        this.name = name;
        this.path = path;
        this.isNode = isNode;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * {@inheritDoc}
     */
    public QName getQName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public boolean denotesNode() {
        return isNode;
    }

    /**
     * {@inheritDoc}
     */
    public Path getPath() {
        return path;
    }
}
