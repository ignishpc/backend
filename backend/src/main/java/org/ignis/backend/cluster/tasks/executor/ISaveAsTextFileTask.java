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
import org.ignis.backend.cluster.tasks.IBarrier;
import org.ignis.backend.exception.IExecutorExceptionWrapper;
import org.ignis.backend.exception.IgnisException;
import org.ignis.rpc.IExecutorException;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;

/**
 * @author César Pomar
 */
public final class ISaveAsTextFileTask extends IExecutorContextTask {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ISaveAsTextFileTask.class);

    public static class Shared {

        public Shared(int executors) {
            partitions = new ArrayList<>(Collections.nCopies(executors, 0l));
            barrier = new IBarrier(executors);
        }

        private final List<Long> partitions;
        private final IBarrier barrier;

    }

    private final Shared shared;
    private final String path;

    public ISaveAsTextFileTask(String name, IExecutor executor, Shared shared, String path) {
        super(name, executor, Mode.LOAD);
        this.shared = shared;
        this.path = path;
    }

    @Override
    public void contextError(IgnisException ex) throws IgnisException {
        throw ex;
    }

    @Override
    public void run(ITaskContext context) throws IgnisException {
        LOGGER.info(log() + "saveAsTextFile started");
        int id = (int) executor.getId();
        try {
            shared.partitions.set(id, executor.getIoModule().partitionCount());
            shared.barrier.await();
            long first = 0;
            for (int i = 0; i < id; i++) {
                first += shared.partitions.get(i);
            }
            executor.getIoModule().saveAsTextFile(path, first);
            shared.barrier.await();
        } catch (IExecutorException ex) {
            shared.barrier.fails();
            throw new IExecutorExceptionWrapper(ex);
        } catch (BrokenBarrierException ex) {
            //Other Task has failed
        } catch (Exception ex) {
            shared.barrier.fails();
            throw new IgnisException(ex.getMessage(), ex);
        }
        LOGGER.info(log() + "saveAsTextFile finished");
    }

}
