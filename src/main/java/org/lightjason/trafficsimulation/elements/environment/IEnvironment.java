/*
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason AgentSpeak(L++) Traffic-Simulation             #
 * # Copyright (c) 2017, LightJason (info@lightjason.org)                               #
 * # This program is free software: you can redistribute it and/or modify               #
 * # it under the terms of the GNU Lesser General Public License as                     #
 * # published by the Free Software Foundation, either version 3 of the                 #
 * # License, or (at your option) any later version.                                    #
 * #                                                                                    #
 * # This program is distributed in the hope that it will be useful,                    #
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of                     #
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                      #
 * # GNU Lesser General Public License for more details.                                #
 * #                                                                                    #
 * # You should have received a copy of the GNU Lesser General Public License           #
 * # along with this program. If not, see http://www.gnu.org/licenses/                  #
 * ######################################################################################
 * @endcond
 */

package org.lightjason.trafficsimulation.elements.environment;

import cern.colt.matrix.DoubleMatrix1D;
import org.lightjason.trafficsimulation.elements.IObject;

import javax.annotation.Nonnull;


/**
 * environment interface
 */
public interface IEnvironment extends IObject<IEnvironment>
{
    /**
     * shutdown execution
     *
     * @return shutdown is enabled
     */
    boolean shutdown();

    /**
     * sets on object inside the grid
     *
     * @param p_object object
     * @param p_position position
     * @return input object or other objects which blocks the position
     */
    @Nonnull
    IObject<?> set( @Nonnull final IObject<?> p_object, @Nonnull final DoubleMatrix1D p_position );

}
