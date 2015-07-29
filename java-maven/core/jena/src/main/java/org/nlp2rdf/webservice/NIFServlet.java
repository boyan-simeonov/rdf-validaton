package org.nlp2rdf.webservice;

import com.hp.hpl.jena.ontology.OntModel;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.aksw.rdfunit.enums.TestCaseExecutionType;
import org.aksw.rdfunit.io.writer.*;
import org.nlp2rdf.core.NIFParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

/**
 * User: hellmann
 * Date: 20.09.13
 */
public abstract class NIFServlet extends HttpServlet {

    private static Logger log = LoggerFactory.getLogger(NIFServlet.class);
    private int counter = 0;

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        handle(httpServletRequest, httpServletResponse);
    }
    
    @Override
    protected void doOptions(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        //The following are CORS headers
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, PUT, POST, OPTIONS, DELETE");
        httpServletResponse.setHeader("Access-Control-Max-Age", "*");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", "*");
    }

    @Override
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
         handle(httpServletRequest, httpServletResponse);
    }

    public abstract OntModel execute(NIFParameters nifParameters) throws Exception;

    /**
     * this method answers GET and POST requests, which are treated the same.
     * - Validates parameters
     * - does the work (execute)
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    private void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

       //The following are CORS headers
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, PUT, POST, OPTIONS, DELETE");
        httpServletResponse.setHeader("Access-Control-Max-Age", "*");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", "*");

        NIFParameters nifParameters = null;
        try {

            //Validate and normalize input
            Monitor mon = MonitorFactory.getTimeMonitor("NIFParameters.getInstance").start();
            String defaultPrefix = httpServletRequest.getRequestURL().toString() + "#";
            nifParameters = NIFParameterWebserviceFactory.getInstance(httpServletRequest, defaultPrefix);
            log.debug("NIFParameters Object created: " + logMonitor(mon.stop()));

            //execute the task
            mon = MonitorFactory.getTimeMonitor("NIFServlet.execute").start();

            OntModel out = execute(nifParameters);
            out.setNsPrefix("p", defaultPrefix);
            log.debug("NIF Component executed task: " + logMonitor(mon.stop()));
            //write the response

            String id = httpServletRequest.getParameter("id");
            if (id == null) id = (String) nifParameters.getOptions().valueOf("id");

            write(httpServletResponse, out, nifParameters.getOutputFormat(), id);
            log.info("output (" + nifParameters.getOutputFormat() + ", " + nifParameters.getOutputFormat() + ") written, triples from input: " + nifParameters.getInputModel().size() + ", added by component: " + out.size());
            writeJamonLog();

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            writeError(e.getMessage(), httpServletResponse);
            /*String msg = e.getMessage() + printParameterMap(httpServletRequest);
            log.error(msg);
            eu.lod2.nlp2rdf.schema.error.Error fatalerror = ErrorHandling.createError(true, requestUrl, msg, model);
            fatalerror.addSource(requestUrl);
            if (nifParameters != null) {
                write(httpServletResponse, model, nifParameters.getFormat());
            } else {
                write(httpServletResponse, model, "rdfxml");
            } */

        } catch (Exception e) {
            e.printStackTrace();
            writeError(e.getMessage(), httpServletResponse);
            /*String msg = "An error occured: " + e.getMessage() + printParameterMap(httpServletRequest);
            log.error(msg, e);
            eu.lod2.nlp2rdf.schema.error.Error fatalerror = ErrorHandling.createError(true, requestUrl, msg, model);
            fatalerror.addSource(requestUrl);
            if (nifParameters != null) {
                write(httpServletResponse, model, nifParameters.getFormat());
            } else {
                write(httpServletResponse, model, "rdfxml");
            }*/
        }
    }

    protected static String logMonitor(Monitor m) {
        return "needed: " + m.getLastValue() + " ms. (" + m.getTotal() + " total)";
    }

    protected void
write(HttpServletResponse httpServletResponse, OntModel out, String format, String id) throws IOException {

        if (id != null) writeFileArchive(id, out);

        //this is the printer where the output has to be on
        OutputStream outputStream = httpServletResponse.getOutputStream();

        //Default writer (RDFUnit)
        RDFWriter outputWriter = null;
        String contentType = "";

        switch (format.toLowerCase()) {

            // treat them the same
            case "turtle":
                outputWriter = new RDFStreamWriter(outputStream, "TURTLE");
                contentType = "text/turtle";
                break;
            case "rdfxml":
                outputWriter = new RDFStreamWriter(outputStream, "RDF/XML");
                contentType = "application/rdf+xml";
                break;
            case "n3":
                outputWriter = new RDFStreamWriter(outputStream, "N3");
                contentType = "text/rdf+n3";
            case "ntriples":
                outputWriter = new RDFStreamWriter(outputStream, "NTRIPLES");
                contentType = "text/rdf+n3";
                break;
            case "html": {
                outputWriter = RDFWriterFactory.createHTMLWriter(TestCaseExecutionType.rlogTestCaseResult, outputStream);
                contentType = "text/html";
                break;
            }
            case "text": {
                contentType = "text";
                break;
            }
            default:
                outputStream.close();
                throw new InvalidParameterException("There is no " + format + " output implemented at the moment. Sorry!");
        }

        httpServletResponse.setContentType(contentType);
        httpServletResponse.setCharacterEncoding("UTF-8");

        try {
            if (outputWriter != null)
                outputWriter.write(out);
            else { // ct -> text
                outputStream.write(outputStream.toString().getBytes());
            }
        } catch (RDFWriterException e) {
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            System.err.println("Cannot write to output: " + e.getMessage());
            e.printStackTrace();
        }


        //RDFWriter writer = out.getWriter(jenaFormat);
        //writer.setProperty("showXmlDeclaration", "true");
        //writer.setProperty("showDoctypeDeclaration", "true");
        //writer.write(out, outputStream, "");
        outputStream.close();


    }

    private void writeFileArchive(String id, OntModel out){
        String simmoID[] = id.split("/");
        File htmlDir = new File(System.getProperty( "catalina.base" ) + "/webapps/validation-results/html/" + simmoID[simmoID.length - 1] + ".html");
        File htmlf = new File(System.getProperty( "catalina.base" ) + "/webapps/validation-results/html");
        File turtleDir = new File(System.getProperty( "catalina.base" ) + "/webapps/validation-results/turtle/" + simmoID[simmoID.length - 1] + ".ttl");
        File turtlef = new File(System.getProperty( "catalina.base" ) + "/webapps/validation-results/turtle");

        if(htmlf.listFiles() != null && htmlf.listFiles().length >= 500) {
            File [] files = htmlDir.listFiles();
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f1.lastModified(), f2.lastModified());
                }
            });
            files[0].delete();
        }

        if(turtlef.listFiles() != null && turtlef.listFiles().length >= 500) {
            File [] files = htmlDir.listFiles();
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f1.lastModified(), f2.lastModified());
                }
            });
            files[0].delete();
        }

        OutputStream fosHtml = null;
        OutputStream fosTurtle = null;
        try {
            fosHtml = new FileOutputStream(htmlDir);
            fosTurtle = new FileOutputStream(turtleDir);
            RDFWriter outputStreamTurtle = new RDFStreamWriter(fosTurtle, "TURTLE");
            RDFWriter outputStreamHTML = RDFWriterFactory.createHTMLWriter(TestCaseExecutionType.rlogTestCaseResult, fosHtml);

            if (outputStreamTurtle != null) {
                try {
                    outputStreamTurtle.write(out);
                } catch (RDFWriterException e) {
                    e.printStackTrace();
                }

                try {
                    outputStreamHTML.write(out);
                } catch (RDFWriterException e) {
                    e.printStackTrace();
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fosHtml != null) try {
                fosHtml.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (fosTurtle != null) {
                try {
                    fosTurtle.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void writeError(String error, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType("text/plain");
        httpServletResponse.setCharacterEncoding("UTF-8");

        //this is the printer where the output has to be on
        PrintWriter pw = httpServletResponse.getWriter();
        pw.println(error);
        pw.close();

    }

    public static String printParameterMap(HttpServletRequest httpServletRequest) {

        log.error("printing map:\n" +
                httpServletRequest.getRequestURL() + "\n" +
                httpServletRequest.getContextPath() + "\n" +
                httpServletRequest + "\n" +
                "parameters: " + httpServletRequest.getParameterMap().keySet() + "\n" +
                "");
        StringBuffer buf = new StringBuffer();
        for (Object key : httpServletRequest.getParameterMap().keySet()) {
            buf.append("\nParameter: " + key + " Values: ");
            for (String s : httpServletRequest.getParameterValues((String) key)) {
                buf.append(((s.length() > 200) ? s.substring(0, 200) + "..." : s) + " ");
            }
        }
        return buf.toString();
    }

    public synchronized void writeJamonLog() {
        counter++;
        if (counter % 100 == 0) {
            try {
                // Create file
                FileWriter fstream = new FileWriter("log/jamonlog.html");
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(MonitorFactory.getReport());
                //Close the output stream
                out.close();
            } catch (Exception e) {//Catch exception if any
                //we don't care
                //System.err.println("Error: " + e.getMessage());
            }
        }
    }

}
