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

import java.util.concurrent.BrokenBarrierException;

/**
 * @author César Pomar
 */
public class IMinTask extends IDriverTask {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IMaxTask.class);

    private final ISource cmp;

    public IMinTask(String name, IExecutor executor, IDriverTask.Shared shared, boolean driver, ISource cmp, ISource tp) {
        super(name, executor, driver ? Mode.SAVE : Mode.LOAD, shared, driver, tp);
        this.cmp = cmp;
    }

    public IMinTask(String name, IExecutor executor, IDriverTask.Shared shared, boolean driver, ISource tp) {
        this(name, executor, shared, driver, null, tp);
    }

    @Override
    public void run(ITaskContext context) throws IgnisException {
        LOGGER.info(log() + "min started");
        try {
            if (!driver) {
                if (cmp != null) {
                    executor.getMathModule().min1(cmp);
                } else {
                    executor.getMathModule().min();
                }
            }
            shared.barrier.await();
            gather(context, true);
        } catch (IgnisException ex) {
            shared.barrier.fails();
            throw ex;
        } catch (BrokenBarrierException ex) {
            //Other Task has failed
        } catch (Exception ex) {
            shared.barrier.fails();
            throw new IgnisException(ex.getMessage(), ex);
        }
        LOGGER.info(log() + "min finished");
    }

}
