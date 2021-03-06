/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.location.jclouds;

import java.util.List;
import java.util.Set;

import org.apache.brooklyn.util.core.config.ConfigBag;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class StubbedComputeServiceRegistry implements ComputeServiceRegistry {

    public static interface NodeCreator {
        public Set<? extends NodeMetadata> createNodesInGroup(String group, int count, Template template) throws RunNodesException;
        public void destroyNode(String id);
        public Set<? extends NodeMetadata> listNodesDetailsMatching(Predicate<ComputeMetadata> filter);
    }

    public static abstract class AbstractNodeCreator implements NodeCreator {
        public final List<NodeMetadata> created = Lists.newCopyOnWriteArrayList();
        public final List<String> destroyed = Lists.newCopyOnWriteArrayList();
        
        @Override
        public Set<? extends NodeMetadata> createNodesInGroup(String group, int count, Template template) throws RunNodesException {
            Set<NodeMetadata> result = Sets.newLinkedHashSet();
            for (int i = 0; i < count; i++) {
                NodeMetadata node = newNode(group, template);
                created.add(node);
                result.add(node);
            }
            return result;
        }
        @Override
        public void destroyNode(String id) {
            destroyed.add(id);
        }
        @Override
        public Set<? extends NodeMetadata> listNodesDetailsMatching(Predicate<ComputeMetadata> filter) {
            return ImmutableSet.of();
        }
        protected abstract NodeMetadata newNode(String group, Template template);
    }

    public static class SingleNodeCreator extends AbstractNodeCreator {
        private final NodeMetadata node;

        public SingleNodeCreator(NodeMetadata node) {
            this.node = node;
        }
        protected NodeMetadata newNode(String group, Template template) {
            return node;
        }
    }

    static class StubbedComputeService extends DelegatingComputeService {
        private final NodeCreator nodeCreator;
        
        public StubbedComputeService(ComputeService delegate, NodeCreator nodeCreator) {
            super(delegate);
            this.nodeCreator = nodeCreator;
        }
        @Override
        public Set<? extends NodeMetadata> createNodesInGroup(String group, int count, Template template) throws RunNodesException {
            return nodeCreator.createNodesInGroup(group, count, template);
        }
        @Override
        public void destroyNode(String id) {
            nodeCreator.destroyNode(id);
        }
        @Override
        public Set<? extends NodeMetadata> listNodesDetailsMatching(Predicate<ComputeMetadata> filter) {
            return nodeCreator.listNodesDetailsMatching(filter);
        }
        @Override
        public Set<? extends NodeMetadata> createNodesInGroup(String group, int count) {
            throw new UnsupportedOperationException();
        }
        @Override
        public Set<? extends NodeMetadata> createNodesInGroup(String group, int count, TemplateOptions templateOptions) {
            throw new UnsupportedOperationException();
        }
        @Override
        public Set<? extends NodeMetadata> destroyNodesMatching(Predicate<NodeMetadata> filter) {
            throw new UnsupportedOperationException();
        }
    }
    
    private final NodeCreator nodeCreator;

    public StubbedComputeServiceRegistry(NodeMetadata node) throws Exception {
        this(new SingleNodeCreator(node));
    }

    public StubbedComputeServiceRegistry(NodeCreator nodeCreator) throws Exception {
        this.nodeCreator = nodeCreator;
    }

    @Override
    public ComputeService findComputeService(ConfigBag conf, boolean allowReuse) {
        ComputeService delegate = ComputeServiceRegistryImpl.INSTANCE.findComputeService(conf, allowReuse);
        return new StubbedComputeService(delegate, nodeCreator);
    }
}
