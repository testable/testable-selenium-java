package io.testable.selenium;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Operations to read from a CSV file while running a test on the Testable platform. When run locally it simply
 * tries to load the file from your local classpath/working directory. It is assumed your CSV file has a header row
 * with column names in the first row.
 */
public class TestableCSVReader {

    private static final String BASE_URL = System.getProperty("TESTABLE_BASE_URL");
    private static final String AGENT_KEY = System.getProperty("TESTABLE_KEY");
    private static final long EXECUTION_ID = Long.getLong("TESTABLE_EXECUTION_ID", -1);
    private static final long CHUNK_ID = Long.getLong("TESTABLE_CHUNK_ID", -1);

    private String name;
    private String nameForIterator;
    private List<CSVRecord> records;
    private int index = 0;

    public TestableCSVReader(String path) throws IOException {
        this.name = path;
        this.nameForIterator = this.name.replace(".", "").replace("/", "");
        InputStream fileIs = this.getClass().getClassLoader().getResourceAsStream(path);
        CSVParser parser = CSVParser.parse(fileIs, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader());
        this.records = parser.getRecords();
    }

    /**
     * Get a row from the file by index (zero based).
     *
     * @param index The row index to load. The first row in the file after the header row is row 0.
     * @return A row record
     */
    public CSVRecord get(int index) {
        return this.records.get(index);
    }

    /**
     * Get a random row from the file.
     *
     * @return A random row record
     */
    public CSVRecord random() {
        int index = (int)Math.floor(Math.random() * records.size());
        return get(index);
    }

    /**
     * Gets the next row in the CSV file. When run on Testable this functions as a global iterator across all regions
     * and test runners. This ensures that the rows in the file are evenly distributed across your virtual users.
     * When the last row is reached the next call globally will return the first row in the file.
     *
     * @return The next row record
     * @throws IOException
     */
    public CSVRecord next() throws IOException {
        return next(true);
    }

    /**
     * Gets the next row in the CSV file. When run on Testable this functions as a global iterator across all regions
     * and test runners. This ensures that the rows in the file are evenly distributed across your virtual users.
     *
     * @param wrap If true once the last row is reached loop back to the first row. If false a
     * {@link ClientProtocolException} is thrown when the end of the file is reached.
     * @return The next row record
     * @throws IOException
     */
    public CSVRecord next(boolean wrap) throws IOException {
        return next(1, wrap).get(0);
    }

    /**
     * Gets the next 1 or more rows from the CSV file. When run on Testable this functions as a global iterator across
     * all regions and test runners. This ensures that the rows in the file are evenly distributed across your
     * virtual users. When the last row is reached the next call globally will return the first row in the file.
     *
     * @param rows Number of rows to return
     * @return A list of the records
     * @throws IOException
     */
    public List<CSVRecord> next(int rows) throws IOException {
        return next(rows, true);
    }

    /**
     * Gets the next 1 or more rows from the CSV file. When run on Testable this functions as a global iterator across
     * all regions and test runners. This ensures that the rows in the file are evenly distributed across your
     * virtual users.
     *
     * @param rows Number of rows to return
     * @param wrap If true once the last row is reached loop back to the first row. If false a
     * {@link ClientProtocolException} is thrown when the end of the file is reached.
     * @return
     * @throws IOException
     */
    public List<CSVRecord> next(int rows, boolean wrap) throws IOException {
        List<CSVRecord> records = new ArrayList<>(rows);
        if (CHUNK_ID < 0) {
            for(int i = 0; i < rows; i++) {
                records.add(get(index));
                if (index == this.records.size() - 1)
                    index = 0;
                else
                    index++;
            }
        } else {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            try {
                this.name.replace(".", "").replace("/", "");
                StringBuilder sb = new StringBuilder();
                sb.append(BASE_URL)
                        .append("/rows/iterators/executions.")
                        .append(EXECUTION_ID)
                        .append(".")
                        .append(nameForIterator)
                        .append("/by-index?wrap=")
                        .append(wrap)
                        .append("&rows=")
                        .append(rows)
                        .append("&length=")
                        .append(this.records.size())
                        .append("&key=")
                        .append(AGENT_KEY);
                HttpGet requestIndices = new HttpGet(sb.toString());
                ResponseHandler<String> responseHandler = response -> {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                };
                String body = httpClient.execute(requestIndices, responseHandler);
                ObjectMapper mapper = new ObjectMapper();
                int[] indices = mapper.readValue(body, int[].class);
                for (int nextIndex : indices) {
                    records.add(get(nextIndex));
                }
            } finally {
                httpClient.close();
            }
        }
        return records;
    }

}
