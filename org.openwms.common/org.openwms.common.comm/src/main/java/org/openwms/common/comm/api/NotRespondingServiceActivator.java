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
package org.openwms.common.comm.api;

import org.openwms.common.comm.Payload;

/**
 * A NotRespondingServiceActivator.
 *
 * @param <T> A type of incoming Payload
 * @author <a href="mailto:scherrer@openwms.org">Heiko Scherrer</a>
 */
public interface NotRespondingServiceActivator<T extends Payload> extends CustomServiceActivator {

    /**
     * Wake up a service, processor or bean an that accepts incoming messages of type <tt>T</tt>.
     *
     * @param message The message to forward
     */
    void wakeUp(T message);
}
