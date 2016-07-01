/*
 * openwms.org, the Open Warehouse Management System.
 * Copyright (C) 2014 Heiko Scherrer
 *
 * This file is part of openwms.org.
 *
 * openwms.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * openwms.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.openwms.common.location;

import java.util.List;

import org.ameba.annotation.TxService;
import org.ameba.exception.NotFoundException;
import org.ameba.exception.ServiceLayerException;
import org.openwms.core.util.TreeNode;
import org.openwms.core.util.TreeNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * A LocationGroupServiceImpl.
 * 
 * @author <a href="mailto:scherrer@openwms.org">Heiko Scherrer</a>
 * @version 0.2
 * @since 0.1
 */
@TxService
class LocationGroupServiceImpl implements LocationGroupService<LocationGroup> {

    private static final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Qualifier("locationGroupDao")
    private LocationGroupRepository locationGroupRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeGroupState(String id, LocationGroupState stateIn, LocationGroupState stateOut) {
        LocationGroup locationGroup = locationGroupRepository.findOne(Long.valueOf(id));
        NotFoundException.throwIfNull(locationGroup, String.format("No LocationGroup with id %s found", id));
        locationGroup.changeState(stateIn, stateOut);
    }

    /**
     * {@inheritDoc}
     */
    //@Override
    public LocationGroup save(LocationGroup locationGroup) {
        if (locationGroup.isNew()) {
            throw new ServiceLayerException("LocationGroup " + locationGroup.getName()
                    + " is new and must be persisted before save");
        }
        LocationGroup persisted = locationGroupRepository.findOne(locationGroup.getPk());
        changeGroupState(persisted, locationGroup);
        return mergeLocationGroup(persisted, locationGroup);
    }

    /**
     * Save changed fields by setting them directly. Merging the instance automatically will not work.
     * 
     * @param persisted
     *            The instance read from the persisted storage
     * @param locationGroup
     *            The new LocationGroup to merge
     * @return The merged persisted object
     */
    protected LocationGroup mergeLocationGroup(LocationGroup persisted, LocationGroup locationGroup) {
        persisted.setDescription(locationGroup.getDescription());
        persisted.setMaxFillLevel(locationGroup.getMaxFillLevel());
        persisted.setLocationGroupCountingActive(locationGroup.isLocationGroupCountingActive());
        return persisted;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    //@Override
    public TreeNode<LocationGroup> getLocationGroupsAsTree() {
        return createTree(new TreeNodeImpl<LocationGroup>(), getLocationGroupsAsList());
    }

    /**
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    //@Override
    public List<LocationGroup> getLocationGroupsAsList() {
        return locationGroupRepository.findAll();
    }

    private TreeNode<LocationGroup> createTree(TreeNode<LocationGroup> root, List<LocationGroup> locationGroups) {
        for (LocationGroup l : locationGroups) {
            searchForNode(l, root);
        }
        return root;
    }

    private TreeNode<LocationGroup> searchForNode(LocationGroup lg, TreeNode<LocationGroup> root) {
        TreeNode<LocationGroup> node;
        if (lg.getParent() == null) {
            node = root.getChild(lg);
            if (node == null) {
                TreeNode<LocationGroup> n1 = new TreeNodeImpl<>();
                n1.setData(lg);
                n1.setParent(root);
                root.addChild(n1.getData(), n1);
                return n1;
            }
            return node;
        } else {
            node = searchForNode(lg.getParent(), root);
            TreeNode<LocationGroup> child = node.getChild(lg);
            if (child == null) {
                child = new TreeNodeImpl<>();
                child.setData(lg);
                child.setParent(node);
                node.addChild(lg, child);
            }
            return child;
        }
    }
}