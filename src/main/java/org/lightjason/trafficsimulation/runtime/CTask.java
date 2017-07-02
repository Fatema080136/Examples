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

package org.lightjason.trafficsimulation.runtime;

import org.lightjason.trafficsimulation.common.CCommon;
import org.lightjason.trafficsimulation.common.CConfiguration;
import org.lightjason.trafficsimulation.elements.IObject;
import org.lightjason.trafficsimulation.elements.environment.CEnvironment;
import org.lightjason.trafficsimulation.elements.environment.IEnvironment;
import org.lightjason.trafficsimulation.ui.CHTTPServer;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * runtime task instance
 */
public class CTask implements ITask
{
    /**
     * logger
     */
    private static final Logger LOGGER = CCommon.logger( ITask.class );
    /**
     * thread
     */
    private final Thread m_thread;

    /**
     * ctor
     */
    public CTask()
    {
        m_thread = new Thread( () ->
        {
            final IEnvironment l_environment;

            try
            (
                final InputStream l_stream = new FileInputStream( CCommon.searchpath( CConfiguration.INSTANCE.get( "agent", "environment", "asl" ) ) )
            )
            {
                l_environment = new CEnvironment.CGenerator( l_stream ).generatesingle();
            }
            catch ( final Exception l_exception )
            {
                System.out.println( l_exception );
                LOGGER.warning( l_exception.getLocalizedMessage() );
                return;
            }

            if ( l_environment == null )
            {
                LOGGER.warning( "environment cannot be instantiable" );
                return;
            }

            // execute objects
            final Set<Callable<?>> l_elements = Collections.synchronizedSet( Stream.of( l_environment ).collect( Collectors.toSet() ) );
            while ( !l_environment.shutdown() )
                l_elements.parallelStream().forEach( CTask::execute );

            CHTTPServer.shutdown();
        } );
    }


    /**
     * execute any object
     *
     * @param p_object callable
     */
    private static void execute( @Nonnull final Callable<?> p_object )
    {
        try
        {
            p_object.call();
        }
        catch ( final Exception l_execution )
        {
            LOGGER.warning( l_execution.getLocalizedMessage() );
        }
    }


    @Override
    public final ITask call() throws Exception
    {
        if ( ( !m_thread.isAlive() ) && ( !m_thread.isInterrupted() ) )
            m_thread.start();

        return this;
    }

    @Override
    public final boolean running()
    {
        return !m_thread.isAlive();
    }
}
