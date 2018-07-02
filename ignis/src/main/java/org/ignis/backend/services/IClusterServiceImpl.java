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

import org.apache.thrift.TException;
import org.ignis.backend.cluster.ICluster;
import org.ignis.rpc.IRemoteException;
import org.ignis.rpc.driver.IClusterService;

/**
 *
 * @author César Pomar
 */
public class IClusterServiceImpl extends IService implements IClusterService.Iface {

    public IClusterServiceImpl(IAttributes attributes) {
        super(attributes);
    }

    @Override
    public long newInstance(long properties) throws IRemoteException, TException {
        long id = attributes.newIdCluster();
        attributes.addCluster(new ICluster(id, attributes.getProperties(properties)));
        return id;
    }

    @Override
    public void keep(long cluster) throws IRemoteException, TException {
        attributes.getCluster(cluster).setKeep(true);
    }

}