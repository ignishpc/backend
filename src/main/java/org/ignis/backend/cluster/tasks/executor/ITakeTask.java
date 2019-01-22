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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.ignis.backend.cluster.IExecutionContext;
import org.ignis.backend.cluster.IExecutor;
import org.ignis.backend.cluster.helpers.IHelper;
import org.ignis.backend.cluster.tasks.IBarrier;
import org.ignis.backend.exception.IgnisException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author César Pomar
 */
public class ITakeTask extends IExecutorContextTask {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ITakeTask.class);

    public static class Shared {

        //Executor -> Count (Multiple Write, One Read)
        private final Map<IExecutor, Long> count = new ConcurrentHashMap<>();
        //Executor -> Result (Multiple Write, One Read)
        private final Map<IExecutor, ByteBuffer> result = new ConcurrentHashMap<>();

    }

    private final List<IExecutor> executors;
    private final IBarrier barrier;
    private final IExecutor driver;
    private final Shared shared;
    private final long n;
    private final boolean ligth;

    public ITakeTask(IHelper helper, IExecutor executor, List<IExecutor> executors, IBarrier barrier, Shared shared,
            IExecutor driver, long n, boolean ligth) {
        super(helper, executor, Mode.LOAD);
        this.executors = executors;
        this.barrier = barrier;
        this.shared = shared;
        this.driver = driver;
        this.n = n;
        this.ligth = ligth;
    }

    @Override
    public void execute(IExecutionContext context) throws IgnisException {
        try {
            if (barrier.await() == 0) {
                shared.count.clear();
                shared.result.clear();
                LOGGER.info(log() + "Executing " + (ligth ? "ligth " : "") + "take");
            }
            barrier.await();
            shared.count.put(executor, executor.getStorageModule().count());
            if (barrier.await() == 0) {
                long elems = n;
                for (IExecutor e : executors) {
                    long ecount = shared.count.get(e);
                    if (ecount > elems) {
                        shared.count.put(e, elems);
                        elems = 0;
                    } else {
                        elems -= ecount;
                    }
                }
                if (elems > 0) {
                    throw new IgnisException("There are not enough elements");
                }
            }
            barrier.await();
            long elems = shared.count.get(executor);
            ByteBuffer bytes;
            if (elems > 0) {
                bytes = executor.getStorageModule().take(executor.getId(), "none", elems, ligth);//TODO
            } else {
                bytes = ByteBuffer.allocate(0);
            }
            if (ligth) {
                shared.result.put(executor, bytes);
            }
            barrier.await();
            if (ligth) {
                ligthMode(context);
            } else {
                directMode(context);
            }
            if (barrier.await() == 0) {
                LOGGER.info(log() + "Take executed");
            }
        } catch (IgnisException ex) {
            barrier.fails();
            throw ex;
        } catch (BrokenBarrierException ex) {
            //Other Task has failed
        } catch (Exception ex) {
            barrier.fails();
            throw new IgnisException(ex.getMessage(), ex);
        }
    }

    private void ligthMode(IExecutionContext context) throws Exception {
        if (barrier.await() == 0) {
            context.set("result", executors.stream().map(e -> shared.result.get(e)).collect(Collectors.toList()));
        }
    }

    private void directMode(IExecutionContext context) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //TODO
    }

}
