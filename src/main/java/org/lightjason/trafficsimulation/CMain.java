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

package org.lightjason.trafficsimulation;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.lightjason.trafficsimulation.common.CCommon;
import org.lightjason.trafficsimulation.common.CConfiguration;
import org.lightjason.trafficsimulation.ui.CHTTPServer;

import java.io.IOException;
import java.util.logging.LogManager;


/**
 * main class
 */
public final class CMain
{
    static
    {
        LogManager.getLogManager().reset();
    }

    /**
     * ctor
     */
    private CMain()
    {}

    /**
     * main method
     * @param p_args command-line parameters
     * @throws IOException error on io errors
     */
    public static void main( final String[] p_args ) throws IOException
    {
        // --- define CLI options ------------------------------------------------------------------------------------------------------------------------------

        final Options l_clioptions = new Options();
        l_clioptions.addOption( "help", false, "shows this information" );
        l_clioptions.addOption( "generateconfig", false, "generate default configuration" );
        l_clioptions.addOption( "config", true, "path to configuration directory (default: <user home>/.asimov/configuration.yaml)" );
        l_clioptions.addOption( "sequential", false, "run simulation in sequential (default is parallel)" );
        l_clioptions.addOption( "iteration", true, "number of iterations" );
        l_clioptions.addOption( "scenariotype", true, "comma-separated list of scenario types (default: xml)" );
        l_clioptions.addOption( "scenario", true, "comma-separated list of scenario files" );

        final CommandLine l_cli;
        try
        {
            l_cli = new DefaultParser().parse( l_clioptions, p_args );
        }
        catch ( final Exception l_exception )
        {
            System.err.println( "command-line arguments parsing error" );
            System.exit( -1 );
            return;
        }



        // --- process CLI arguments and initialize configuration ----------------------------------------------------------------------------------------------

        if ( l_cli.hasOption( "help" ) )
        {
            new HelpFormatter().printHelp( new java.io.File( CMain.class.getProtectionDomain().getCodeSource().getLocation().getPath() ).getName(), l_clioptions );
            return;
        }

        if ( l_cli.hasOption( "generateconfig" ) )
        {
            System.out.println( CCommon.languagestring( CMain.class, "generateconfig", CConfiguration.createdefault() ) );
            return;
        }

        if ( !l_cli.hasOption( "scenario" ) )
        {
            System.out.println( CCommon.languagestring( CMain.class, "noscenario", CConfiguration.createdefault() ) );
            System.exit( -1 );
            return;
        }

        // load configuration
        CConfiguration.INSTANCE.loadfile( l_cli.getOptionValue( "config", "" ) );

        // start http server if possible
        CHTTPServer.execute();
    }

}
