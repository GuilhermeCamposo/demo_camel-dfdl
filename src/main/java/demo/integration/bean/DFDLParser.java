package demo.integration.bean;

import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.camel.Exchange;
import org.apache.daffodil.japi.Compiler;
import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.Diagnostic;
import org.apache.daffodil.japi.ParseResult;
import org.apache.daffodil.japi.ProcessorFactory;
import org.apache.daffodil.japi.infoset.JDOMInfosetOutputter;
import org.apache.daffodil.japi.io.InputSourceDataInputStream;
import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DFDLParser {
	
	private DataProcessor dataProcessor;
	private static final Logger LOGGER = LoggerFactory.getLogger(DFDLParser.class);
	
	@PostConstruct
	private void init() throws Exception {
		URL schemaFileURL = DFDLParser.class.getResource("/parser.dfdl.xsd");
	
		// First compile the DFDL Schema
		Compiler c = Daffodil.compiler();
		ProcessorFactory pf = c.compileSource(schemaFileURL.toURI());
		
		if (pf.isError()) {
			// didn't compile schema. Must be diagnostic of some sort.
			List<Diagnostic> diags = pf.getDiagnostics();
			for (Diagnostic d : diags) {
				LOGGER.error(d.getSomeMessage());
			}
			throw new Exception("compilation error");
		}
		
		 dataProcessor = pf.onPath("/");
		
		if (dataProcessor.isError()) {
			// didn't compile schema. Must be diagnostic of some sort.
			List<Diagnostic> diags = dataProcessor.getDiagnostics();
			for (Diagnostic d : diags) {
				LOGGER.error(d.getSomeMessage());
			}
			throw new Exception("data processor compilation error");
		}
		
	}

	public void parseToXML(Exchange exchange)  throws Exception {
		
		LOGGER.info("**** Parsing data into XML *****");
		
		InputSourceDataInputStream dis = new InputSourceDataInputStream( (java.io.InputStream) exchange.getIn().getBody());
		
		// Setup JDOM outputter
		JDOMInfosetOutputter outputter = new JDOMInfosetOutputter();

		// Do the parse
		ParseResult res = dataProcessor.parse(dis, outputter);

		// Check for errors
		if (res.isError()) {
			// didn't parse the data. Must be diagnostic of some sort.
			List<Diagnostic> diags = res.getDiagnostics();
			for (Diagnostic d : diags) {
				LOGGER.error(d.getSomeMessage());
			}
			throw new Exception("parsing error");
		}

		Document doc = outputter.getResult();
	
		// if we get here, we have a parsed infoset result!
		// Let's print the XML infoset.
		//
		// Note that if we had only wanted this text, we could have used
		// a different outputter to create XML text directly,
		// but below we're going to transform this JDOM tree.
		XMLOutputter xo = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
		Writer writer = new StringWriter();
		xo.output(doc, writer);
		
		exchange.getIn().setBody(writer.toString());
	}
	
}
