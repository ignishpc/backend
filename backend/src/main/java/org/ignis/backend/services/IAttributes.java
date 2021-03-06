/*
 * Copyright (C) 2018
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ignis.backend.services;

import org.ignis.backend.cluster.ICluster;
import org.ignis.backend.cluster.IDriver;
import org.ignis.backend.cluster.ISSH;
import org.ignis.backend.exception.IgnisException;
import org.ignis.properties.IKeys;
import org.ignis.properties.IProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author César Pomar
 */
public final class IAttributes {

    private static final Pattern DEFAULT_CLUSTER_NAMES = Pattern.compile("Cluster\\([0-9]+\\)");

    public final IProperties defaultProperties;
    public final ISSH ssh;
    public final IDriver driver;
    private final List<ICluster> clusterList;
    private final List<IProperties> propertiesList;

    public IAttributes(IProperties defaultProperties) {
        this.defaultProperties = defaultProperties;
        this.clusterList = new ArrayList<>();
        this.propertiesList = new ArrayList<>();
        this.ssh = new ISSH(defaultProperties.getInteger(IKeys.DRIVER_RPC_PORT) + 1,//backend + 1
                defaultProperties.getInteger(IKeys.EXECUTOR_RPC_PORT) + 1, //ssh server + 1
                defaultProperties.getProperty(IKeys.DRIVER_PRIVATE_KEY, null),
                defaultProperties.getProperty(IKeys.DRIVER_PUBLIC_KEY, null));
        this.driver = new IDriver(defaultProperties.getInteger(IKeys.EXECUTOR_RPC_PORT), defaultProperties);
    }

    public IProperties getProperties(long id) throws IgnisException {
        synchronized (propertiesList) {
            if (propertiesList.size() > id) {
                return propertiesList.get((int) id);
            }
        }
        throw new IgnisException("Properties doesn't exist");
    }

    public long addProperties(IProperties properties) {
        synchronized (propertiesList) {
            propertiesList.add(properties);
            return propertiesList.size() - 1;
        }
    }

    public ICluster getCluster(long id) throws IgnisException {
        synchronized (clusterList) {
            if (clusterList.size() > id) {
                return clusterList.get((int) id);
            }
        }
        throw new IgnisException("Cluster doesn't exist");
    }

    public void changeClusterName(long id, String name) throws IgnisException {
        Set<String> names;
        synchronized (clusterList) {
            names = clusterList.stream().map(c -> c.getName()).collect(Collectors.toSet());
        }
        if (names.contains(name) || DEFAULT_CLUSTER_NAMES.matcher(name).matches()) {
            throw new IgnisException("Cluster must be unique");
        }
        ICluster cluster = getCluster(id);
        synchronized (cluster.getLock()) {
            cluster.setName(name);
        }
    }

    public long newCluster() {
        synchronized (clusterList) {
            clusterList.add(null);
            return clusterList.size() - 1;
        }
    }

    public void setCluster(ICluster cluster) {
        synchronized (clusterList) {
            clusterList.set((int) cluster.getId(), cluster);
        }
    }

    public Collection<ICluster> getClusters() {
        return clusterList;
    }

}
