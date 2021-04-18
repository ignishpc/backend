/*
 *
 *  * Copyright (C) 2021 César Pomar
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.ignis.backend.cluster.tasks.executor;

import org.apache.thrift.TException;
import org.ignis.backend.cluster.IExecutor;
import org.ignis.backend.cluster.ITaskContext;
import org.ignis.backend.exception.IExecutorExceptionWrapper;
import org.ignis.backend.exception.IgnisException;
import org.ignis.rpc.IExecutorException;
import org.ignis.rpc.ISource;
import org.slf4j.LoggerFactory;

public class IVoidCallTask extends IExecutorContextTask {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IVoidCallTask.class);

    private final ISource function;
    private final boolean hasArgument;

    public IVoidCallTask(String name, IExecutor executor, ISource function, boolean hasArgument) {
        super(name, executor, hasArgument ? Mode.LOAD : Mode.NONE);
        this.function = function;
        this.hasArgument = hasArgument;
    }

    @Override
    public void run(ITaskContext context) throws IgnisException {
        LOGGER.info(log() + "call started");
        try {
            if (hasArgument) {
                executor.getGeneralActionModule().foreachExecutor(function);
            } else {
                executor.getGeneralActionModule().execute(function);
            }
        } catch (IExecutorException ex) {
            throw new IExecutorExceptionWrapper(ex);
        } catch (TException ex) {
            throw new IgnisException(ex.getMessage(), ex);
        }
        LOGGER.info(log() + "call finished");
    }
}
