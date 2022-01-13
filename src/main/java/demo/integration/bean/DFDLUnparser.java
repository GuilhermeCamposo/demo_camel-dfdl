package demo.integration.bean;

import java.io.StringReader;
import java.net.URL;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.camel.Exchange;
import org.apache.daffodil.japi.Compiler;
import org.apache.daffodil.japi.Daffodil;
import org.apache.daffodil.japi.DataProcessor;
import org.apache.daffodil.japi.Diagnostic;
import org.apache.daffodil.japi.ProcessorFactory;
import org.apache.daffodil.japi.UnparseResult;
import org.apache.daffodil.japi.infoset.JDOMInfosetInputter;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DFDLUnparser {
	
	private DataProcessor dataProcessor;
	private static final Logger LOGGER = LoggerFactory.getLogger(DFDLUnparser.class);
	
	@PostConstruct
	private void init() throws Exception {
		URL schemaFileURL = DFDLUnparser.class.getResource("/unparser.dfdl.xsd");
	
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

	public void unparse(Exchange exchange) throws Exception  {
		
		LOGGER.info("**** Unparsing XML infoset back into data *****");
		
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(new StringReader(exchange.getIn().getBody(String.class)));

		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		java.nio.channels.WritableByteChannel wbc = java.nio.channels.Channels.newChannel(bos);
		JDOMInfosetInputter inputter = new JDOMInfosetInputter(doc);
		UnparseResult res2 = dataProcessor.unparse(inputter, wbc);

		if (res2.isError()) {
			// didn't unparse. Must be diagnostic of some sort.
			List<Diagnostic> diags = res2.getDiagnostics();
			for (Diagnostic d : diags) {
				LOGGER.error(d.getSomeMessage());
			}
			throw new Exception("parsing error");
		}

		byte[] ba = bos.toByteArray();

		String encoding = "utf-8"; 

		java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(ba);
		java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(bis, encoding));
		
		LOGGER.info("Data as text in encoding " + encoding);
		
		StringBuilder sbuilder = new StringBuilder();
		String line;
		
		while ((line = r.readLine()) != null) {
			sbuilder.append(line).append("\n");
		}

		exchange.getIn().setBody(sbuilder.toString());
	}
	

}
