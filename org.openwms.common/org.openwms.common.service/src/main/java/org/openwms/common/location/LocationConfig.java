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

import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

/**
 * A LocationConfig is a Spring managed configuration class the defines a bean to load a few Locations upfront.
 *
 * @author <a href="mailto:scherrer@openwms.org">Heiko Scherrer</a>
 */
@Configuration
class LocationConfig {

    @Profile("default")
    @Bean
    @DependsOn("locationGroupRunner")
    CommandLineRunner locationRunner(LocationRepository lr) {
        return args -> {
            lr.deleteAll();
            Stream.of("INIT/0000/0000/0000/0000,ERR_/0000/0000/0000/0000,EXT_/0000/0000/0000/0000,AKL_/0001/0000/0000/0000".split(","))
                    .forEach(x -> lr.save(new Location(LocationPK.fromString(x))));
        };
    }
}
