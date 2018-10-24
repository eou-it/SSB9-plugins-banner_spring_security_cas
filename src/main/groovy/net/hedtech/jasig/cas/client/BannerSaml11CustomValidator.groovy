package net.hedtech.jasig.cas.client

import org.jasig.cas.client.authentication.AttributePrincipal
import org.jasig.cas.client.authentication.AttributePrincipalImpl
import org.jasig.cas.client.util.CommonUtils
import org.jasig.cas.client.util.SamlUtils
import org.jasig.cas.client.validation.AbstractUrlBasedTicketValidator
import org.jasig.cas.client.validation.Assertion
import org.jasig.cas.client.validation.AssertionImpl
import org.jasig.cas.client.validation.TicketValidationException
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Interval
import org.opensaml.Configuration
import org.opensaml.DefaultBootstrap
import org.opensaml.common.IdentifierGenerator
import org.opensaml.common.impl.SecureRandomIdentifierGenerator
import org.opensaml.saml1.core.*
import org.opensaml.ws.soap.soap11.Envelope
import org.opensaml.xml.ConfigurationException
import org.opensaml.xml.io.Unmarshaller
import org.opensaml.xml.io.UnmarshallerFactory
import org.opensaml.xml.io.UnmarshallingException
import org.opensaml.xml.parse.BasicParserPool
import org.opensaml.xml.parse.XMLParserException
import org.opensaml.xml.schema.XSAny
import org.opensaml.xml.schema.XSString
import org.w3c.dom.Document
import org.w3c.dom.Element

import javax.xml.validation.Schema
import java.nio.charset.Charset
import java.text.DateFormat
import java.text.SimpleDateFormat

public final class BannerSaml11CustomValidator extends AbstractUrlBasedTicketValidator {
    private long tolerance = 75000L;
    private final BasicParserPool basicParserPool = new BasicParserPool()
    private final IdentifierGenerator identifierGenerator;


    public BannerSaml11CustomValidator(final String casServerUrlPrefix) {
        super(casServerUrlPrefix);
        this.basicParserPool.setNamespaceAware(true);
        try {
            this.identifierGenerator = new SecureRandomIdentifierGenerator();
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }
    }


    protected String getUrlSuffix() {
        return "samlValidate";
    }

    protected void populateUrlAttributeMap(Map<String, String> urlParameters) {
        String service = (String)urlParameters.get("service");
        urlParameters.remove("service");
        urlParameters.remove("ticket");
        urlParameters.put("TARGET", service);
    }

    protected void setDisableXmlSchemaValidation(boolean disabled) {
        if (disabled) {
            this.basicParserPool.setSchema((Schema)null);
        }

    }

    protected byte[] getBytes(String text) {
        try {
            return CommonUtils.isNotBlank(this.getEncoding()) ? text.getBytes(this.getEncoding()) : text.getBytes();
        } catch (Exception var3) {
            return text.getBytes();
        }
    }

    protected Assertion parseResponseFromServer(String response) throws TicketValidationException {
        try {
            Document responseDocument = this.basicParserPool.parse(new ByteArrayInputStream(this.getBytes(response)));
            Element responseRoot = responseDocument.getDocumentElement();
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(responseRoot);
            Envelope envelope = (Envelope)unmarshaller.unmarshall(responseRoot);
            Response samlResponse = (Response)envelope.getBody().getOrderedChildren().get(0);
            List<org.opensaml.saml1.core.Assertion> assertions = samlResponse.getAssertions();
            if (assertions.isEmpty()) {
                throw new TicketValidationException("No assertions found.");
            }

            Iterator i$ = assertions.iterator();

            while(i$.hasNext()) {
                org.opensaml.saml1.core.Assertion assertion = (org.opensaml.saml1.core.Assertion)i$.next();
                if (this.isValidAssertion(assertion)) {
                    AuthenticationStatement authenticationStatement = this.getSAMLAuthenticationStatement(assertion);
                    if (authenticationStatement == null) {
                        throw new TicketValidationException("No AuthentiationStatement found in SAML Assertion.");
                    }

                    Subject subject = authenticationStatement.getSubject();
                    if (subject == null) {
                        throw new TicketValidationException("No Subject found in SAML Assertion.");
                    }

                    List<Attribute> attributes = this.getAttributesFor(assertion, subject);
                    Map<String, Object> personAttributes = new HashMap();
                    Iterator i1$ = attributes.iterator();

                    while(i1$.hasNext()) {
                        Attribute samlAttribute = (Attribute)i1$.next();
                        List<?> values = this.getValuesFrom(samlAttribute);
                        personAttributes.put(samlAttribute.getAttributeName(), values.size() == 1 ? values.get(0) : values);
                    }

                    AttributePrincipal principal = new AttributePrincipalImpl(subject.getNameIdentifier().getNameIdentifier(), personAttributes);
                    Map<String, Object> authenticationAttributes = new HashMap();
                    authenticationAttributes.put("samlAuthenticationStatement::authMethod", authenticationStatement.getAuthenticationMethod());
                    DateTime notBefore = assertion.getConditions().getNotBefore();
                    DateTime notOnOrAfter = assertion.getConditions().getNotOnOrAfter();
                    DateTime authenticationInstant = authenticationStatement.getAuthenticationInstant();
                    return new AssertionImpl(principal, notBefore.toDate(), notOnOrAfter.toDate(), authenticationInstant.toDate(), authenticationAttributes);
                }
            }
        } catch (UnmarshallingException var20) {
            throw new TicketValidationException(var20);
        } catch (XMLParserException var21) {
            throw new TicketValidationException(var21);
        }

        throw new TicketValidationException("No Assertion found within valid time range.  Either there's a replay of the ticket or there's clock drift. Check tolerance range, or server/client synchronization.");
    }

    private boolean isValidAssertion(org.opensaml.saml1.core.Assertion assertion) {
        DateTime notBefore = assertion.getConditions().getNotBefore();
        DateTime notOnOrAfter = assertion.getConditions().getNotOnOrAfter();
        if (notBefore != null && notOnOrAfter != null) {
            DateTime currentTime = new DateTime(DateTimeZone.UTC);
            Interval validityRange = new Interval(notBefore.minus(this.tolerance), notOnOrAfter.plus(this.tolerance));
            if (validityRange.contains(currentTime)) {
                this.logger.debug("Current time is within the interval validity.");
                return true;
            } else if (currentTime.isBefore(validityRange.getStart())) {
                this.logger.debug("skipping assertion that's not yet valid...");
                return false;
            } else {
                this.logger.debug("skipping expired assertion...");
                return false;
            }
        } else {
            this.logger.debug("Assertion has no bounding dates. Will not process.");
            return false;
        }
    }

    private AuthenticationStatement getSAMLAuthenticationStatement(org.opensaml.saml1.core.Assertion assertion) {
        List<AuthenticationStatement> statements = assertion.getAuthenticationStatements();
        return statements.isEmpty() ? null : (AuthenticationStatement)statements.get(0);
    }

    private List<Attribute> getAttributesFor(org.opensaml.saml1.core.Assertion assertion, Subject subject) {
        List<Attribute> attributes = new ArrayList();
        Iterator i$ = assertion.getAttributeStatements().iterator();

        while(i$.hasNext()) {
            AttributeStatement attribute = (AttributeStatement)i$.next();
            if (subject.getNameIdentifier().getNameIdentifier().equals(attribute.getSubject().getNameIdentifier().getNameIdentifier())) {
                attributes.addAll(attribute.getAttributes());
            }
        }

        return attributes;
    }

    private List<?> getValuesFrom(Attribute attribute) {
        List<Object> list = new ArrayList();
        Iterator i$ = attribute.getAttributeValues().iterator();

        while(i$.hasNext()) {
            Object o = i$.next();
            if (o instanceof XSAny) {
                list.add(((XSAny)o).getTextContent());
            } else if (o instanceof XSString) {
                list.add(((XSString)o).getValue());
            } else {
                list.add(o.toString());
            }
        }

        return list;
    }

    protected String retrieveResponseFromServer(URL validationUrl, String ticket) {
        String MESSAGE_TO_SEND = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body><samlp:Request xmlns:samlp=\"urn:oasis:names:tc:SAML:1.0:protocol\"  MajorVersion=\"1\" MinorVersion=\"1\" RequestID=\"" + this.identifierGenerator.generateIdentifier() + "\" IssueInstant=\"" + SamlUtils.formatForUtcTime(new Date()) + "\">" + "<samlp:AssertionArtifact>" + ticket + "</samlp:AssertionArtifact></samlp:Request></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        /*String MESSAGE_TO_SEND = """<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns="urn:oasis:names:tc:SAML:1.0:protocol">
    <soap:Header/>
    <soap:Body>
        <Request MajorVersion="1" MinorVersion="1" RequestID="%s" IssueInstant="%s">
            <AssertionArtifact>$ticket</AssertionArtifact>
        </Request>
    </soap:Body>
</soap:Envelope>""";
        MESSAGE_TO_SEND = String.format(
                MESSAGE_TO_SEND,
                generateId(),
                SamlUtils.formatForUtcTime(new Date())
                );
*/
        println "MESSAGE_TO_SEND =============== $MESSAGE_TO_SEND"
        HttpURLConnection conn = null;
        DataOutputStream out = null;
        BufferedReader in1 = null;

        try {
            conn = this.getURLConnectionFactory().buildHttpURLConnection(validationUrl.openConnection());
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setRequestProperty("Content-Length", Integer.toString(MESSAGE_TO_SEND.length()));
            conn.setRequestProperty("SOAPAction", "http://www.oasis-open.org/committees/security");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(MESSAGE_TO_SEND);
            out.flush();
            in1 = new BufferedReader(CommonUtils.isNotBlank(this.getEncoding()) ? new InputStreamReader(conn.getInputStream(), Charset.forName(this.getEncoding())) : new InputStreamReader(conn.getInputStream()));
            StringBuilder buffer = new StringBuilder(256);

            String line;
            while((line = in1.readLine()) != null) {
                buffer.append(line);
            }

            String var9 = buffer.toString();
            return var9;
        } catch (IOException var13) {
            throw new RuntimeException(var13);
        } finally {
            CommonUtils.closeQuietly(out);
            CommonUtils.closeQuietly(in1);
            if (conn != null) {
                conn.disconnect();
            }

        }
    }

    public void setTolerance(long tolerance) {
        this.tolerance = tolerance;
    }

    static {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException var1) {
            throw new RuntimeException(var1);
        }
    }

/*    public static String formatForUtcTime(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }*/
}