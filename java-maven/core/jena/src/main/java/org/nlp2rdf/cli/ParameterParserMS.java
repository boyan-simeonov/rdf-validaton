package org.nlp2rdf.cli;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import joptsimple.OptionSet;
import org.apache.jena.riot.RiotParseException;
import org.nlp2rdf.core.Format;
import org.nlp2rdf.core.NIFParameters;
import org.nlp2rdf.core.Text2RDF;
import org.nlp2rdf.core.urischemes.URIScheme;
import org.nlp2rdf.core.urischemes.URISchemeHelper;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by boyan on 15-6-25.
 */
public class ParameterParserMS extends ParameterParser{
    /**
     * Parses the NIF options into an object, note that "start" and "port" have
     * to be treated separately
     *
     * @param options
     * @param isWebService
     *            options are parsed from a webservices
     * @return
     * @throws IOException
     * @throws ParameterException
     */
    public static NIFParameters parseOptions(OptionSet options,
                                             boolean isWebService, String input) throws IOException, ParameterException {
        OntModel model = ModelFactory.createOntologyModel(
                OntModelSpec.OWL_DL_MEM, ModelFactory.createDefaultModel());

        String inputtype = (String) options.valueOf("t");
        String outformat = (String) options.valueOf("o");
        String informat = (String) options.valueOf("f");
        URIScheme uriScheme = URISchemeHelper.getInstance((String) options
                .valueOf("f"));

        /** Implementation check **/
        switch (informat) {
            case "turtle":
                break;
            case "rdfxml":
                break;
            case "text":
                break;
            default:
                throw new ParameterException("informat=" + informat
                        + " not implemented yet");
        }

        String in = (String) options.valueOf("i");

        if (input.isEmpty() && !in.isEmpty()) input = in;
//        if (isWebService && (!options.hasArgument("i") || !input.isEmpty())) {
//            throw new ParameterException(
//                    "Parameter input=$data was not set properly");
//        }

        InputStream is = null;
        try {

            if (inputtype.equals("direct")) {
                if (input == null) {
                    throw new ParameterException(
                            "input can not be empty, on CLI use '-i -  for stdin' or curl --data-urlencode @-");
                } else if (input.equals("-")) {
                    is = new BufferedInputStream(System.in);
                } else {
                    if (isWebService) {
                        is = new ByteArrayInputStream(input.getBytes());
                    } else {
                        // this is a workaround to build a more robust cli,
                        // which shows mercy for forgetting the -t option
                        System.err
                                .println("you forgot the \"-t file\" or \"-t url\" option, but I am ok, assuming \"-t file\"");
                        if (new File(input).exists()) {
                            inputtype = "file";
                        }
                    }
                }
            }

            if (inputtype.equals("file")) {
                is = new FileInputStream(new File(input));
            } else if (inputtype.equals("url")) {
                is = new URI(input).toURL().openStream();
            } else if (inputtype.equals("direct")) {
                is = new ByteArrayInputStream(input.getBytes());
            } else {
                throw new ParameterException("Option --intype=" + inputtype
                        + " not known, use direct|file|url");
            }
        } catch (FileNotFoundException fne) {
            fne.printStackTrace();
            throw new ParameterException(
                    "ERROR: file not found, maybe you have to switch --intype=url, file="
                            + input);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new ParameterException(
                    "ERROR: malformed URL in parameter input=" + input);
        }

        // case -l parameter setted, language detection will be enabled.
        if (informat.equals("text")) {
            new Text2RDF().createContextIndividual(
                    (String) options.valueOf("p"), toString(is), uriScheme,
                    model);

        } else {
            try {
                model.read(is, "", Format.toJena(informat));
            } catch (NullPointerException e) {
                throw new ParameterException(
                        "an error has occured while reading informat="
                                + informat + ", intype=" + inputtype
                                + " and input=" + input.substring(0, 20)
                                + "...", e);
            } catch (RiotParseException rpe) {
                throw new ParameterException(
                        "The RDF in the format "
                                + informat
                                + "is not well formed, please check parameter intype and informat",
                        rpe);
            }
        }

        NIFParameters np = new NIFParameters(model, options,
                (String) options.valueOf("p"), (String) options.valueOf("lp"),
                uriScheme, null, outformat);
        // set additional parameters
        np.setConfig((String) options.valueOf("config"));
        return np;
    }
}
