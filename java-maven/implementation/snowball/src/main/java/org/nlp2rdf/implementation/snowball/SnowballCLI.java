package org.nlp2rdf.implementation.snowball;

import java.io.IOException;

import org.nlp2rdf.cli.ParameterException;
import org.nlp2rdf.cli.ParameterParser;
import org.nlp2rdf.core.Format;
import org.nlp2rdf.core.NIFParameters;
import org.nlp2rdf.core.vocab.NIFOntClasses;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * User: hellmann
 * Date: 08.09.13
 */
public class SnowballCLI {
    public static void main(String[] args) throws IOException {
        OptionParser parser =  ParameterParser.getParser(args, "http://cli.nlp2rdf.org/snowball#");
        // TODO as a courtesy to windows users
        // parser.acceptsAll(asList("outfile"), "a NIF RDF file with the result of validation as RDF, only takes effect, if outformat is 'turtle' or 'rdfxml'").withRequiredArg().ofType(File.class).describedAs("RDF file");
        ParameterParser.addCLIParameter(parser);
        
        SnowballWrapper s = new SnowballWrapper();
     
		try {
            OptionSet options = ParameterParser.getOption(parser, args);
				ParameterParser.handleHelpAndWS(options, "");
            NIFParameters nifParameters = ParameterParser.parseOptions(options, false);

            //customize
            OntModel model = nifParameters.getInputModel();
        
            //some stats
            Monitor mon = MonitorFactory.getTimeMonitor(s.getClass().getCanonicalName()).start();
            
            int x = 0;
            for (ExtendedIterator<Individual> it = model.listIndividuals(NIFOntClasses.Context.getOntClass(model)); it.hasNext(); ) {
                Individual context = it.next();
                s.processText(context, model, model, nifParameters);
                x++;
            }
            model.write(System.out, Format.toJena(nifParameters.getOutputFormat()));
        
            } catch (Exception e) {
            	// TODO Auto-generated catch block
            	e.printStackTrace();
            }
    }
}
