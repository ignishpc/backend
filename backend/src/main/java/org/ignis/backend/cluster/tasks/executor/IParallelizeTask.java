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
package org.ignis.backend.cluster.tasks.executor;

import org.ignis.backend.cluster.IExecutor;
import org.ignis.backend.cluster.ITaskContext;
import org.ignis.backend.exception.IgnisException;
import org.ignis.rpc.ISource;
import org.slf4j.LoggerFactory;

/**
 * @author César Pomar
 */
public class IParallelizeTask extends IDriverTask {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IParallelizeTask.class);
    private final long partitions;

    public IParallelizeTask(String name, IExecutor executor, Shared shared, boolean driver, long partitions) {
        super(name, executor, driver ? Mode.LOAD : Mode.SAVE, shared, driver);
        this.partitions = partitions;
    }

    public IParallelizeTask(String name, IExecutor executor, Shared shared, boolean driver, long partitions, ISource src) {
        super(name, executor, driver ? Mode.LOAD : Mode.SAVE, shared, driver, src);
        this.partitions = partitions;
    }

    @Override
    public void run(ITaskContext context) throws IgnisException {
        LOGGER.info(log() + "Executing parallelize");
        try {
            scatter(context, partitions);
        } catch (IgnisException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IgnisException(ex.getMessage(), ex);
        }
        LOGGER.info(log() + "Parallelize executed");
    }

}
