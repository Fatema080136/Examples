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

package org.lightjason.trafficsimulation.elements;

import cern.colt.matrix.DoubleMatrix1D;
import org.apache.commons.lang3.tuple.Pair;
import org.lightjason.agentspeak.action.IAction;
import org.lightjason.agentspeak.agent.IBaseAgent;
import org.lightjason.agentspeak.beliefbase.view.IView;
import org.lightjason.agentspeak.common.CCommon;
import org.lightjason.agentspeak.configuration.IAgentConfiguration;
import org.lightjason.agentspeak.generator.IBaseAgentGenerator;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ILiteral;
import org.lightjason.trafficsimulation.ui.CHTTPServer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * base agent object
 *
 * @tparam T agent type
 */
public abstract class IBaseObject<T extends IObject<?>> extends IBaseAgent<T> implements IObject<T>
{
    /**
     * serial id
     */
    private static final long serialVersionUID = 6278806527768825298L;
    /**
     * current position of the agent
     */
    protected DoubleMatrix1D m_position;
    /**
     * functor definition
     */
    private final String m_functor;
    /**
     * id of the object
     */
    private final String m_id;
    /**
     * reference to external beliefbase
     */
    private final IView m_external;



    /**
     * ctor
     *
     * @param p_configuration agent configuration
     * @param p_functor functor of the object literal
     * @param p_id name of the object
     * @param p_position initial position
     */
    protected IBaseObject( final IAgentConfiguration<T> p_configuration, final String p_functor, final String p_id, final DoubleMatrix1D p_position )
    {
        super( p_configuration );
        m_functor = p_functor;
        m_id = p_id;
        m_position = p_position;

        //m_beliefbase.add( new CEnvironmentBeliefbase().create( "env", m_beliefbase ) );
        m_external = m_beliefbase.beliefbase().view( "extern" );
    }

    @Override
    public final String id()
    {
        return m_id;
    }

    @Override
    public final Stream<ILiteral> literal( final IObject<?>... p_object )
    {
        return this.literal( Arrays.stream( p_object ) );
    }

    @Override
    public final Stream<ILiteral> literal( final Stream<IObject<?>> p_object )
    {
        return Stream.of(
            CLiteral.from(
                m_functor,
                Stream.concat(
                    Stream.concat(
                        Stream.of(
                            CLiteral.from( "id", CRawTerm.from( m_id ) )
                        ),
                        m_external.stream().map( i -> i.shallowcopysuffix() )
                    ),
                    this.individualliteral( p_object ).sorted().sequential()
                )
            )
        );
    }

    /**
     * define object literal addons
     *
     * @param p_object calling objects
     * @return literal stream
     */
    protected abstract Stream<ILiteral> individualliteral( final Stream<IObject<?>> p_object );

    @Override
    public final int hashCode()
    {
        return m_id.hashCode();
    }

    @Override
    public final boolean equals( final Object p_object )
    {
        return ( p_object != null ) && ( p_object instanceof IObject<?> ) && ( this.hashCode() == p_object.hashCode() );
    }

    @Override
    public final DoubleMatrix1D position()
    {
        return m_position;
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * base agent generator
     *
     * @tparam T agent type
     */
    protected abstract static class IBaseGenerator<T extends IObject<?>> extends IBaseAgentGenerator<T> implements IGenerator<T>
    {
        /**
         * @param p_stream stream
         * @param p_actions action
         * @throws Exception on any error
         */
        protected IBaseGenerator( final InputStream p_stream, final Stream<IAction> p_actions, final Class<? extends T> p_agentclass ) throws Exception
        {
            super( p_stream, Stream.concat( p_actions, CCommon.actionsFromAgentClass( p_agentclass ) ).collect( Collectors.toSet() ) );
        }

        @Override
        public final T generatesingle( final Object... p_data )
        {
            return CHTTPServer.register( this.generate( p_data ) );
        }

        /**
         * generates the agent
         *
         * @param p_data creating arguments
         * @return agent object and group names
         */
        protected abstract Pair<T, Stream<String>> generate( final Object... p_data );
    }

}
