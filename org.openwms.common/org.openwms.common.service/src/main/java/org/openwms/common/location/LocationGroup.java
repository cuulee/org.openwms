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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.ameba.exception.ServiceLayerException;
import org.openwms.common.StateChangeException;
import org.springframework.util.Assert;

/**
 * A LocationGroup is a logical group of {@code Location}s, grouping together {@code Location}s with same characteristics.
 *
 * @author <a href="mailto:scherrer@openwms.org">Heiko Scherrer</a>
 * @version 1.0
 * @GlossaryTerm
 * @see org.openwms.common.location.Location
 * @since 0.1
 */
@Entity
@Table(name = "COM_LOCATION_GROUP")
public class LocationGroup extends Target implements Serializable {

    /** Unique identifier of a {@code LocationGroup}. */
    @Column(name = "C_NAME", unique = true, nullable = false, length = LENGTH_NAME)
    private String name;
    /** Length of the name field; used for telegram mapping and for column definition. */
    public static final int LENGTH_NAME = 20;

    /** Description for the {@code LocationGroup}. */
    @Column(name = "C_DESCRIPTION")
    private String description;

    /** A type can be assigned to a {@code LocationGroup}. */
    @Column(name = "C_GROUP_TYPE")
    private String groupType;

    /**
     * Is the {@code LocationGroup} included in the calculation of {@code TransportUnit}s? <p> {@literal true} : Location is included in the
     * calculation of {@code TransportUnit}s.<br> {@literal false}: Location is not included in the calculation of {@code TransportUnit}s.
     * </p>
     */
    @Column(name = "C_GROUP_COUNTING_ACTIVE")
    private boolean locationGroupCountingActive = true;

    /**
     * Number of {@code Location}s belonging to the {@code LocationGroup}.
     */
    @Column(name = "C_NO_LOCATIONS")
    private int noLocations = 0;

    /**
     * State of infeed.
     */
    @Column(name = "C_GROUP_STATE_IN")
    @Enumerated(EnumType.STRING)
    private LocationGroupState groupStateIn = LocationGroupState.AVAILABLE;

    /**
     * References the {@code LocationGroup} that locked this {@code LocationGroup} for infeed.
     */
    @ManyToOne
    @JoinColumn(name = "C_IN_LOCKER")
    private LocationGroup stateInLocker;

    /**
     * State of outfeed.
     */
    @Column(name = "C_GROUP_STATE_OUT")
    @Enumerated(EnumType.STRING)
    private LocationGroupState groupStateOut = LocationGroupState.AVAILABLE;

    /**
     * References the {@code LocationGroup} that locked this {@code LocationGroup} for outfeed.
     */
    @ManyToOne
    @JoinColumn(name = "C_OUT_LOCKER")
    private LocationGroup stateOutLocker;

    /**
     * Maximum fill level of the {@code LocationGroup}.
     */
    @Column(name = "C_MAX_FILL_LEVEL")
    private float maxFillLevel = 0;

    /**
     * Name of the PLC system, tied to this {@code LocationGroup}.
     */
    @Column(name = "C_SYSTEM_CODE")
    private String systemCode;

    /* ------------------- collection mapping ------------------- */
    /**
     * Parent {@code LocationGroup}.
     */
    @ManyToOne
    @JoinColumn(name = "C_PARENT")
    private LocationGroup parent;

    /**
     * Child {@code LocationGroup}s.
     */
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.ALL})
    private Set<LocationGroup> locationGroups = new HashSet<>();

    /**
     * Child {@link Location}s.
     */
    @OneToMany(mappedBy = "locationGroup")
    private Set<Location> locations = new HashSet<>();

    /*~ ----------------------------- constructors ------------------- */

    /**
     * Dear JPA...
     */
    protected LocationGroup() {
    }

    /**
     * Create a new {@code LocationGroup} with an unique name.
     *
     * @param name The name of the {@code LocationGroup}
     */
    public LocationGroup(String name) {
        Assert.hasText(name, "Creation of LocationGroup with name null");
        this.name = name;
    }

    /*~ ----------------------------- methods ------------------- */

    /**
     * Returns the name of the {@code LocationGroup}.
     *
     * @return The name of the {@code LocationGroup}
     */
    public String getName() {
        return name;
    }

    /**
     * Check whether infeed is allowed for the {@code LocationGroup}.
     *
     * @return {@literal true} if allowed, otherwise {@literal false}.
     */
    public boolean isInfeedAllowed() {
        return (getGroupStateIn() == LocationGroupState.AVAILABLE);
    }

    /**
     * Check whether infeed of the {@code LocationGroup} is blocked.
     *
     * @return {@literal true} if blocked, otherwise {@literal false}.
     */
    public boolean isInfeedBlocked() {
        return !isInfeedAllowed();
    }

    /**
     * Check whether outfeed is allowed for the {@code LocationGroup}.
     *
     * @return {@literal true} if allowed, otherwise {@literal false}.
     */
    public boolean isOutfeedAllowed() {
        return (getGroupStateIn() == LocationGroupState.AVAILABLE);
    }

    /**
     * Check whether outfeed of the {@code LocationGroup} is blocked.
     *
     * @return {@literal true} if blocked, otherwise {@literal false}.
     */
    public boolean isOutfeedBlocked() {
        return !isInfeedAllowed();
    }

    /**
     * Returns the infeed state of the {@code LocationGroup}.
     *
     * @return The state of infeed
     */
    public LocationGroupState getGroupStateIn() {
        return this.groupStateIn;
    }

    /**
     * Change the infeed state of the {@code LocationGroup}.
     *
     * @param newGroupStateIn The state to set
     */
    public void setGroupStateIn(LocationGroupState newGroupStateIn) {
        if (stateInLocker != null) {
            throw new StateChangeException("The LocationGroup's state is blocked by any other LocationGroup and cannot be changed");
        }
        groupStateIn = newGroupStateIn;
        locationGroups.forEach(lg -> lg.setGroupStateIn(newGroupStateIn, this));
    }

    /**
     * Change the infeed state of the {@code LocationGroup}.
     *
     * @param newGroupStateIn The state to set
     * @param lockLG The {@code LocationGroup} that wants to lock/unlock this {@code LocationGroup}.
     */
    private void setGroupStateIn(LocationGroupState newGroupStateIn, LocationGroup lockLG) {
        if (groupStateIn == LocationGroupState.NOT_AVAILABLE && newGroupStateIn == LocationGroupState.AVAILABLE) {

            // unlock
            stateInLocker = null;
        }
        if (groupStateIn == LocationGroupState.AVAILABLE && newGroupStateIn == LocationGroupState.NOT_AVAILABLE) {

            // lock
            stateInLocker = lockLG;
        }
        groupStateIn = newGroupStateIn;
        locationGroups.forEach(lg -> lg.setGroupStateIn(newGroupStateIn, lockLG));
    }

    /**
     * Return the outfeed state of the {@code LocationGroup}.
     *
     * @return The state of outfeed
     */
    public LocationGroupState getGroupStateOut() {
        return groupStateOut;
    }

    /**
     * Set the outfeed state of the {@code LocationGroup}.
     *
     * @param gStateOut The state to set
     * @param lockLg The {@code LocationGroup} that wants to lock/unlock this {@code LocationGroup}.
     */
    public void setGroupStateOut(LocationGroupState gStateOut, LocationGroup lockLg) {
        if (this.groupStateOut == LocationGroupState.NOT_AVAILABLE && gStateOut == LocationGroupState.AVAILABLE
                && (this.stateOutLocker == null || this.stateOutLocker.equals(lockLg))) {
            this.groupStateOut = gStateOut;
            this.stateOutLocker = null;
            for (LocationGroup child : locationGroups) {
                child.setGroupStateOut(gStateOut, lockLg);
            }
        }
        if (this.groupStateOut == LocationGroupState.AVAILABLE && gStateOut == LocationGroupState.NOT_AVAILABLE
                && (this.stateOutLocker == null || this.stateOutLocker.equals(lockLg))) {
            this.groupStateOut = gStateOut;
            this.stateOutLocker = lockLg;
            for (LocationGroup child : locationGroups) {
                child.setGroupStateOut(gStateOut, lockLg);
            }
        }
    }

    /**
     * Returns the count of all sub {@link Location}s.
     *
     * @return The count of {@link Location}s belonging to this {@code LocationGroup}
     */
    public int getNoLocations() {
        return this.noLocations;
    }

    /**
     * Returns the maximum fill level of the {@code LocationGroup}.<br> The maximum fill level defines how many {@link Location}s of the
     * {@code LocationGroup} can be occupied by {@code TransportUnit}s. <p> The maximum fill level is a value between 0 and 1 and represents
     * a percentage value. </p>
     *
     * @return The maximum fill level
     */
    public float getMaxFillLevel() {
        return this.maxFillLevel;
    }

    /**
     * Set the maximum fill level for the {@code LocationGroup}. <p> Pass a value between 0 and 1.<br> For example maxFillLevel = 0.85
     * means: 85% of all {@link Location}s can be occupied. </p>
     *
     * @param maxFillLevel The maximum fill level
     */
    public void setMaxFillLevel(float maxFillLevel) {
        this.maxFillLevel = maxFillLevel;
    }

    /**
     * Returns the type of the {@code LocationGroup}.
     *
     * @return The type of the {@code LocationGroup}
     */
    public String getGroupType() {
        return this.groupType;
    }

    /**
     * Set the type for the {@code LocationGroup}.
     *
     * @param groupType The type of the {@code LocationGroup}
     */
    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    /**
     * Returns the description text.
     *
     * @return The Description as String
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Set the description text.
     *
     * @param description The String to set as description text
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the parent {@code LocationGroup}.
     *
     * @return The parent {@code LocationGroup}
     */
    public LocationGroup getParent() {
        return this.parent;
    }

    /**
     * Set the parent {@code LocationGroup}.
     *
     * @param parent The {@code LocationGroup} to set as parent
     */
    private void setParent(LocationGroup parent) {
        this.parent = parent;
    }

    /**
     * Return all child {@code LocationGroup}.
     *
     * @return A set of all {@code LocationGroup} having this one as parent
     */
    public Set<LocationGroup> getLocationGroups() {
        return locationGroups;
    }

    /**
     * Add a {@code LocationGroup} to the list of children.
     *
     * @param locationGroup The {@code LocationGroup} to be added as a child
     * @return {@literal true} if the {@code LocationGroup} was new in the collection of {@code LocationGroup}s, otherwise {@literal false}
     */
    public boolean addLocationGroup(LocationGroup locationGroup) {
        if (locationGroup == null) {
            throw new IllegalArgumentException("LocationGroup to be added is null");
        }
        if (locationGroup.parent != null) {
            locationGroup.parent.removeLocationGroup(locationGroup);
        }
        locationGroup.parent = this;
        locationGroup.setGroupStateIn(groupStateIn, this);
        locationGroup.setGroupStateOut(groupStateOut, this);
        return locationGroups.add(locationGroup);
    }

    /**
     * Remove a {@code LocationGroup} from the list of children.
     *
     * @param locationGroup The {@code LocationGroup} to be removed from the list of children
     * @return {@literal true} if the {@code LocationGroup} was found and could be removed, otherwise {@literal false}
     */
    public boolean removeLocationGroup(LocationGroup locationGroup) {
        Assert.notNull(locationGroup, "LocationGroup to remove is null. this: " + this);
        locationGroup.setParent(null);
        return locationGroups.remove(locationGroup);
    }

    /**
     * Return all {@link Location}s.
     *
     * @return {@link Location}s
     */
    public Set<Location> getLocations() {
        return locations;
    }

    /**
     * Check whether this {@code LocationGroup} has {@code Location}s assigned.
     *
     * @return {@literal true} if {@code Location}s are assigned, otherwise {@literal false}
     */
    public boolean hasLocations() {
        return locations != null && !locations.isEmpty();
    }

    /**
     * Add a {@link Location} to the list of children.
     *
     * @param location The {@link Location} to be added as child
     * @return {@literal true} if the {@link Location} was new in the collection of {@link Location}s, otherwise {@literal false}
     */
    public boolean addLocation(Location location) {
        Assert.notNull(location, "Location to be added to LocationGroup is null. this: " + this);
        location.setLocationGroup(this);
        return locations.add(location);
    }

    /**
     * Remove a {@link Location} from the list of children.
     *
     * @param location The {@link Location} to be removed from the list of children
     * @return {@literal true} if the {@link Location} was found and could be removed, otherwise {@literal false}
     */
    public boolean removeLocation(Location location) {
        Assert.notNull(location, "Location to remove from LocationGroup is null. this: " + this);
        location.unsetLocationGroup();
        return locations.remove(location);
    }

    /**
     * Returns the systemCode.
     *
     * @return The systemCode
     */
    public String getSystemCode() {
        return systemCode;
    }

    /**
     * Set the systemCode.
     *
     * @param systemCode The systemCode to set
     */
    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    /**
     * Returns the locationGroupCountingActive.
     *
     * @return The locationGroupCountingActive
     */
    public boolean isLocationGroupCountingActive() {
        return locationGroupCountingActive;
    }

    /**
     * Set the locationGroupCountingActive.
     *
     * @param locationGroupCountingActive The locationGroupCountingActive to set
     */
    public void setLocationGroupCountingActive(boolean locationGroupCountingActive) {
        this.locationGroupCountingActive = locationGroupCountingActive;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 111;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocationGroup)) {
            return false;
        }
        LocationGroup other = (LocationGroup) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Return the name of the {@code LocationGroup} as String.
     *
     * @return String
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Tries to change the {@code groupStateIn} and {@code groupStateOut} of the {@code LocationGroup}. A state change is only allowed when
     * the parent {@code LocationGroup}s state is not blocked.
     *
     * @param stateIn The new groupStateIn to set, or {@literal null}
     * @param stateOut The new groupStateOut to set, or {@literal null}
     */
    void changeState(LocationGroupState stateIn, LocationGroupState stateOut) {
        if (groupStateIn != stateIn && stateIn != null) {
            // GroupStateIn changed
            if (parent != null
                    && parent.getGroupStateIn() == LocationGroupState.NOT_AVAILABLE
                    && groupStateIn == LocationGroupState.AVAILABLE) {
                throw new ServiceLayerException(
                        "Not allowed to change GroupStateIn, parent locationGroup is not available");
            }
            setGroupStateIn(stateIn, this);
        }
        if (groupStateOut != stateOut && stateOut != null) {
            // GroupStateOut changed
            if (parent != null
                    && parent.getGroupStateOut() == LocationGroupState.NOT_AVAILABLE
                    && groupStateOut == LocationGroupState.AVAILABLE) {
                throw new ServiceLayerException(
                        "Not allowed to change GroupStateOut, parent locationGroup is not available");
            }
            setGroupStateOut(stateOut, this);
        }
    }

    /**
     *
     * @return
     */
    public boolean hasParent() {
        return parent != null;
    }
}