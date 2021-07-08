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
import org.ignis.rpc.ISource;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author César Pomar
 */
public final class IJoinTask extends IExecutorContextTask {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IJoinTask.class);

    public static class Shared {

        public Shared(int executors) {
            this.executors = executors;
            barrier = new IBarrier(executors);
            partitions = new AtomicLong(0);
        }

        private final IBarrier barrier;
        private final int executors;
        private AtomicLong partitions;

    }

    private final ISource src;
    private final Long numPartitions;
    private final Shared shared;

    public IJoinTask(String name, IExecutor executor, Shared shared, ISource src) {
        this(name, executor, shared, null, src);
    }

    public IJoinTask(String name, IExecutor executor, Shared shared) {
        this(name, executor, shared, null, null);
    }

    public IJoinTask(String name, IExecutor executor, Shared shared, Long numPartitions) {
        this(name, executor, shared, numPartitions, null);
    }


    public IJoinTask(String name, IExecutor executor, Shared shared, Long numPartitions, ISource src) {
        super(name, executor, Mode.LOAD_AND_SAVE);
        this.shared = shared;
        this.src = src;
        this.numPartitions = numPartitions;
    }

    @Override
    public void run(ITaskContext context) throws IgnisException {
        LOGGER.info(log() + "join started");
        try {
            long np;
            if (numPartitions == null) {
                shared.partitions.set(0);
                shared.barrier.await();
                shared.partitions.addAndGet(executor.getIoModule().partitionCount());
                shared.barrier.await();
                np = shared.partitions.get();
            }else{
                np = numPartitions;
            }
            context.loadContextAsVariable(executor, "other");
            if (src != null) {
                executor.getGeneralModule().join3("other", np, src);
            } else {
                executor.getGeneralModule().join("other", np);
            }
        } catch (IExecutorException ex) {
            shared.barrier.fails();
            throw new IExecutorExceptionWrapper(ex);
        } catch (BrokenBarrierException ex) {
            //Other Task has failed
        } catch (Exception ex) {
            shared.barrier.fails();
            throw new IgnisException(ex.getMessage(), ex);
        }
        LOGGER.info(log() + "join finished");
    }

}
