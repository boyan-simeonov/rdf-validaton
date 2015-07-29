/***************************************************************************/
/*  Copyright (C) 2010-2011, Sebastian Hellmann                            */
/*  Note: If you need parts of NLP2RDF in another licence due to licence   */
/*  incompatibility, please mail hellmann@informatik.uni-leipzig.de        */
/*                                                                         */
/*  This file is part of NLP2RDF.                                          */
/*                                                                         */
/*  NLP2RDF is free software; you can redistribute it and/or modify        */
/*  it under the terms of the GNU General Public License as published by   */
/*  the Free Software Foundation; either version 3 of the License, or      */
/*  (at your option) any later version.                                    */
/*                                                                         */
/*  NLP2RDF is distributed in the hope that it will be useful,             */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of         */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the           */
/*  GNU General Public License for more details.                           */
/*                                                                         */
/*  You should have received a copy of the GNU General Public License      */
/*  along with this program. If not, see <http://www.gnu.org/licenses/>.   */
/***************************************************************************/

package org.nlp2rdf.webservice;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.nlp2rdf.cli.ParameterException;
import org.nlp2rdf.cli.ParameterParser;
import org.nlp2rdf.cli.ParameterParserMS;
import org.nlp2rdf.core.NIFParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * User: Sebastian Hellmann
 * See   http://persistence.uni-leipzig.org/nlp2rdf/specification/api.html
 */
public class NIFParameterWebserviceFactory {
    private static Logger log = LoggerFactory.getLogger(NIFParameterWebserviceFactory.class);


    /**
     * Factory method
     *
     * @param httpServletRequest
     * @return
     */
    public static NIFParameters getInstance(HttpServletRequest httpServletRequest, String defaultPrefix) throws ParameterException, IOException {

        //twice the size to split key value
        String[] args = new String[httpServletRequest.getParameterMap().size() * 2];


        int x = 0;

        for (Object key : httpServletRequest.getParameterMap().keySet()) {
            String pname = (String) key;
            //transform key to CLI style
            pname = (pname.length() == 1) ? "-" + pname : "--" + pname;

            //collect CLI args
            args[x++] = pname;
            args[x++] = httpServletRequest.getParameter((String) key);

        }


        String line;
        String content = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpServletRequest.getInputStream()));


            while((line = reader.readLine()) != null){
                if (line.startsWith("---") || line.startsWith("Content") || line.startsWith("html")) continue;
                content += line;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (args.length == 0 && !content.isEmpty()) {
            args = new String[4];
            String [] params = content.split("&");
            if (params.length > 0) {
                String [] i = params[0].split("=");
                String [] id = params[1].split("=");
                if (i.length > 0) {
                    args[0] =  "--" + i[0];
                    args[1] = URLDecoder.decode(i[1], "UTF-8");
                }
                if (id.length > 0) {
                    args[2] = "-" + id[0];
                    args[3] = URLDecoder.decode(id[1], "UTF-8");
                }
                content = "";
            }
        }

//        System.out.println(content);


        //parse CLI args
        OptionParser parser = ParameterParser.getParser(args, defaultPrefix);
        OptionSet options = ParameterParser.getOption(parser, args);


        // print help screen
        if (options.has("h")) {
            String addHelp = "";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            parser.printHelpOn(baos);
            throw new ParameterException(baos.toString());
        }


        // parse options with webservice setted to true
        return ParameterParserMS.parseOptions(options, true, content);
    }
}
