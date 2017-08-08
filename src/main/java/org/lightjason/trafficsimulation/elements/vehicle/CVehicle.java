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

package org.lightjason.trafficsimulation.elements.vehicle;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import com.codepoetics.protonpack.StreamUtils;
import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.lightjason.agentspeak.action.binding.IAgentAction;
import org.lightjason.agentspeak.action.binding.IAgentActionFilter;
import org.lightjason.agentspeak.action.binding.IAgentActionName;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.beliefbase.CBeliefbase;
import org.lightjason.agentspeak.beliefbase.IBeliefbaseOnDemand;
import org.lightjason.agentspeak.beliefbase.storage.CSingleOnlyStorage;
import org.lightjason.agentspeak.beliefbase.view.IView;
import org.lightjason.agentspeak.configuration.IAgentConfiguration;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ILiteral;
import org.lightjason.agentspeak.language.execution.IVariableBuilder;
import org.lightjason.agentspeak.language.instantiable.IInstantiable;
import org.lightjason.agentspeak.language.instantiable.plan.trigger.CTrigger;
import org.lightjason.agentspeak.language.instantiable.plan.trigger.ITrigger;
import org.lightjason.agentspeak.language.variable.CConstant;
import org.lightjason.agentspeak.language.variable.IVariable;
import org.lightjason.trafficsimulation.common.CCommon;
import org.lightjason.trafficsimulation.common.CMath;
import org.lightjason.trafficsimulation.common.EDirection;
import org.lightjason.trafficsimulation.elements.CUnit;
import org.lightjason.trafficsimulation.elements.IBaseObject;
import org.lightjason.trafficsimulation.elements.IObject;
import org.lightjason.trafficsimulation.elements.environment.IEnvironment;
import org.lightjason.trafficsimulation.ui.api.CAnimation;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * vehicle agent
 */
@IAgentAction
public final class CVehicle extends IBaseObject<IVehicle> implements IVehicle
{
    /**
     * serial id
     */
    private static final long serialVersionUID = 3822143462033345857L;
    /**
     * literal functor
     */
    private static final String FUNCTOR = "vehicle";
    /**
     * vehicle type
     */
    private final ETYpe m_type;
    /**
     * environment
     */
    private final IEnvironment m_environment;
    /**
     * accelerate speed in m/sec^2
     * @warning must be in (0, infinity)
     */
    @Nonnegative
    private final double m_accelerate;
    /**
     * decelerate speed in m/sec^2
     * @warning must be in (0, infinity)
     */
    private final double m_decelerate;
    /**
     * maximum speed
     */
    private final double m_maximumspeed;
    /**
     * current speed in km/h
     */
    private final AtomicDouble m_speed = new AtomicDouble( );
    /**
     * panelize value
     */
    private final AtomicDouble m_panelize = new AtomicDouble();
    /*
     * current position on lane / cell position
     */
    private final DoubleMatrix1D m_position;
    /**
     * goal position (x-coordinate)
     */
    private final int m_goal;

    /**
     * ctor
     *
     * @param p_configuration agent configuration
     * @param p_id name of the object
     * @param p_start start position
     * @param p_goal goal position (x-coordinate)
     * @param p_acceleration accelerate speed
     * @param p_deceleration decelerate speed
     */
    private CVehicle( @Nonnull final IAgentConfiguration<IVehicle> p_configuration, @Nonnull final String p_id,
                      @Nonnull final IEnvironment p_environment, @Nonnull final ETYpe p_type,
                      @Nonnull final DoubleMatrix1D p_start, @Nonnegative final int p_goal,
                      @Nonnegative final double p_maximumspeed, @Nonnegative final double p_acceleration, @Nonnegative final double p_deceleration
    )
    {
        super( p_configuration, FUNCTOR, p_id );

        if ( p_maximumspeed < 120 )
            throw new RuntimeException( "maximum speed to low" );

        if ( p_acceleration < 1 )
            throw new RuntimeException( "acceleration is to low" );

        if ( p_deceleration < 1 )
            throw new RuntimeException( "deceleration is to low" );

        if ( p_deceleration < p_acceleration )
            throw new RuntimeException( "deceleration should be greater than acceleration" );


        m_type = p_type;
        m_environment = p_environment;

        m_position = p_start;
        m_goal = p_goal;

        m_maximumspeed = p_maximumspeed;
        m_accelerate = p_acceleration;
        m_decelerate = p_deceleration;

        // beliefbase
        final IView l_env = new CBeliefbase( new CSingleOnlyStorage<>() ).create( "env", m_beliefbase );
        m_beliefbase.add( l_env );

        l_env.add(
            new CEnvironmentView(
                Collections.unmodifiableSet( CMath.cellangle( 5, 135, 225 ).collect( Collectors.toSet() ) )
            ).create( "backward", l_env ) );

        l_env.add( new CEnvironmentView(
            Collections.unmodifiableSet(
                Stream.concat(
                    CMath.cellangle( 8, 0, 60 ),
                    CMath.cellangle( 8, 300, 359.99 )
                ).collect( Collectors.toSet() )
            )
        ).create( "forward", l_env ) );

        CAnimation.CInstance.INSTANCE.send( EStatus.INITIALIZE, this );
    }

    @Nonnull
    @Override
    public final DoubleMatrix1D position()
    {
        return m_position;
    }

    @Nonnull
    @Override
    public final DoubleMatrix1D nextposition()
    {
        return EDirection.FORWARD.position(
            this.position(),
            new DenseDoubleMatrix1D( new double[]{this.position().get( 0 ), m_goal} ),
            CUnit.INSTANCE.speedtocell( this.speed() ).doubleValue()
        );
    }

    @Nonnull
    @Override
    public final IObject<IVehicle> release()
    {
        CAnimation.CInstance.INSTANCE.send( EStatus.RELEASE, this );
        return this;
    }

    @Override
    public final Map<String, Object> map( @Nonnull final EStatus p_status )
    {
        return StreamUtils.zip(
            Stream.of( "type", "status", "id", "y", "x", "goal", "speed", "maxspeed", "acceleration", "deceleration" ),
            Stream.of( this.type().toString(),
                       p_status.toString(),
                       this.id(),
                       this.position().get( 0 ),
                       this.position().get( 1 ),
                       m_goal,
                       m_speed.get(),
                       m_maximumspeed,
                       m_accelerate,
                       m_decelerate
            ),
            ImmutablePair::new
        ).collect( Collectors.toMap( ImmutablePair::getLeft, ImmutablePair::getRight ) );
    }

    @Override
    protected final Stream<ILiteral> individualliteral( final IObject<?> p_object )
    {
        return Stream.of(
            CLiteral.from( "speed", CRawTerm.from( m_speed.get() ) ),
            CLiteral.from( "lane", CRawTerm.from( m_position.get( 0 ) ) ),
            CLiteral.from( "distance", CRawTerm.from( CMath.distance( m_position, p_object.position() ) ) )
        );
    }

    @Override
    @Nonnegative
    public final double penalty()
    {
        return m_panelize.get();
    }

    @Override
    @Nonnegative
    public final double acceleration()
    {
        return m_accelerate;
    }

    @Override
    @Nonnegative
    public final double deceleration()
    {
        return m_decelerate;
    }

    @Override
    public final double speed()
    {
        return m_speed.get();
    }

    @Nonnull
    @Override
    public final IVehicle penalty( @Nonnull final Number p_value )
    {
        m_panelize.addAndGet( p_value.doubleValue() );
        return this;
    }

    @Override
    public final ETYpe type()
    {
        return m_type;
    }

    @Override
    public final IVehicle call() throws Exception
    {
        super.call();

        //if ( m_type.equals( ETYpe.USERVEHICLE ) )
        //    System.out.println( this.beliefbase().stream().collect( Collectors.toList() ) );

        // give environment the data if it is a user car
        if ( !m_environment.move( this ) )
            this.oncollision();

        return this;
    }

    /**
     * runs collision handling
     */
    private void oncollision()
    {
        if ( m_type.equals( ETYpe.USERVEHICLE ) )
            m_environment.trigger( CTrigger.from( ITrigger.EType.ADDGOAL, CLiteral.from( "vehicle/usercollision" ) ) );
        else
            this.trigger( CTrigger.from( ITrigger.EType.ADDGOAL, CLiteral.from( "vehicle/collision" ) ) );
    }



    // --- agent actions ---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * accelerate
     */
    @IAgentActionFilter
    @IAgentActionName( name = "vehicle/accelerate" )
    private void accelerate( final Number p_strength )
    {
        final double l_value = m_speed.get() + CUnit.INSTANCE.accelerationtospeed(
            m_accelerate * Math.max( 0, Math.min( 1, p_strength.doubleValue() ) )
        ).doubleValue();

        if (  l_value > m_maximumspeed )
            throw new RuntimeException( MessageFormat.format( "cannot increment speed: {0}", this ) );

        m_speed.set( l_value );
    }

    /**
     * decelerate
     */
    @IAgentActionFilter
    @IAgentActionName( name = "vehicle/decelerate" )
    private void decelerate( final Number p_strength )
    {
        final double l_value = m_speed.get() - CUnit.INSTANCE.accelerationtospeed(
            m_decelerate * Math.max( 0, Math.min( 1, p_strength.doubleValue() ) )
        ).doubleValue();

        if (  l_value < 0 )
            throw new RuntimeException( MessageFormat.format( "cannot decrement speed: {0}", this ) );

        m_speed.set( l_value );
    }

    /**
     * swing-out
     */
    @IAgentActionFilter
    @IAgentActionName( name = "vehicle/swingout" )
    private void swingout()
    {
        final Number l_lane = m_position.get( 0 ) + ( m_goal == 0 ? 1 : -1 );
        if ( !m_environment.lanechange( this, l_lane.intValue() ) )
            this.oncollision();
    }

    /**
     * go back into lane
     */
    @IAgentActionFilter
    @IAgentActionName( name = "vehicle/goback" )
    private void goback()
    {
        final Number l_lane = m_position.get( 0 ) + ( m_goal == 0 ? -1 : 1 );
        if ( !m_environment.lanechange( this, l_lane.intValue() ) )
            this.oncollision();
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * generator
     * @see https://en.wikipedia.org/wiki/Orders_of_magnitude_(acceleration)
     */
    public static final class CGenerator extends IBaseGenerator<IVehicle> implements Callable<IVehicle>
    {
        /**
         * counter
         */
        private static final AtomicLong COUNTER = new AtomicLong();
        /**
         * visibility within the UI
         */
        private final boolean m_visible;
        /**
         * vehicle type
         */
        private final ETYpe m_type;


        /**
         * generator
         *
         * @param p_stream stream
         * @param p_uiaccessiable generated cars are ui-accessable
         * @throws Exception on any error
         */
        public CGenerator( @Nonnull final InputStream p_stream, final boolean p_uiaccessiable, final ETYpe p_type ) throws Exception
        {
            super( p_stream, CVehicle.class, new CVariableBuilder() );
            m_visible = p_uiaccessiable;
            m_type = p_type;
        }

        @Override
        public final IGenerator<IVehicle> resetcount()
        {
            COUNTER.set( 0 );
            return this;
        }

        @Override
        public final IVehicle call() throws Exception
        {
            return this.generatesingle();
        }

        @Nullable
        @Override
        @SuppressWarnings( "unchecked" )
        protected final Triple<IVehicle, Boolean, Stream<String>> generate( @Nullable final Object... p_data )
        {
            if ( ( p_data == null ) || ( p_data.length < 6 ) )
                throw new RuntimeException( CCommon.languagestring( this, "parametercount" ) );

            return new ImmutableTriple<>(
                new CVehicle(
                    m_configuration,
                    MessageFormat.format( "{0} {1}", FUNCTOR, COUNTER.getAndIncrement() ),
                    (IEnvironment) p_data[0],
                    m_type,

                    (DoubleMatrix1D) p_data[1],
                    ( (Number) p_data[2] ).intValue(),

                    ( (Number) p_data[3] ).doubleValue(),
                    ( (Number) p_data[4] ).doubleValue(),
                    ( (Number) p_data[5] ).doubleValue()
                ),
                m_visible,
                Stream.of( FUNCTOR )
            );
        }
    }


    /**
     * variable builder of vehicle
     */
    private static class CVariableBuilder implements IVariableBuilder
    {

        @Override
        public final Stream<IVariable<?>> apply( final IAgent<?> p_agent, final IInstantiable p_instance )
        {
            final IVehicle l_vehicle = p_agent.<IVehicle>raw();

            return Stream.of(
                new CConstant<>( "CurrentSpeed", l_vehicle.speed() ),
                new CConstant<>( "Acceleration", l_vehicle.acceleration() ),
                new CConstant<>( "Deceleration", l_vehicle.deceleration() )
            );
        }
    }

    /**
     * on-demand beliefbase
     */
    private final class CEnvironmentView extends IBeliefbaseOnDemand<IVehicle>
    {
        /**
         * cell position
         */
        private final Set<DoubleMatrix1D> m_position;


        /**
         * ctor
         *
         * @param p_position cell position relative to object position
         */
        CEnvironmentView( final Set<DoubleMatrix1D> p_position )
        {
            m_position = p_position;
        }

        @Nonnull
        @Override
        public final Stream<ILiteral> streamLiteral()
        {
            return m_position.parallelStream()
                             .map( m_environment::get )
                             .filter( Objects::nonNull )
                             .collect(
                                 Collectors.toMap(
                                     i -> CMath.distance( i.position(), CVehicle.this.position() ),
                                     i -> i.literal( CVehicle.this )
                                 )
                             ).values().stream();
        }

        @Override
        public final boolean containsLiteral( @Nonnull final String p_key )
        {
            return "vehicle".equals( p_key );
        }

        @Nonnull
        @Override
        public final Collection<ILiteral> literal( @Nonnull final String p_key )
        {
            return this.streamLiteral().filter( i -> p_key.equals( i.functor() ) ).collect( Collectors.toList() );
        }

    }
}
