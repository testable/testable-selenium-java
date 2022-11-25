package io.testable.selenium;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

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

    private static final int GLOBAL_CLIENT_INDEX = Integer.getInteger("TESTABLE_GLOBAL_CLIENT_INDEX", 0);
    private static final int ITERATION = Integer.getInteger("TESTABLE_ITERATION", 0);
    private static final int CONCURRENT_CLIENTS = Integer.getInteger("TESTABLE_CONCURRENT_CLIENTS", 1);

    private List<CSVRecord> records;
    private int index = CONCURRENT_CLIENTS * ITERATION + GLOBAL_CLIENT_INDEX;

    public TestableCSVReader(String path) throws IOException {
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
        return this.records.get(index % this.records.size());
    }

    /**
     * Get a row from the file by index (zero based).
     *
     * @param index The row index to load. The first row in the file after the header row is row 0.
     * @param wrap If true once the last row is reached loop back to the first row. If false a
     *        RuntimeException is thrown when the end of the file is reached.
     * @return A row record
     */
    public CSVRecord get(int index, boolean wrap) {
        if (!wrap && index >= this.records.size())
            throw new RuntimeException("End of CSV reached");
        return this.records.get(index % this.records.size());
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
     *             RuntimeException is thrown when the end of the file is reached.
     * @return The next row record
     */
    public CSVRecord next(boolean wrap) {
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
     *             RuntimeException is thrown when the end of the file is reached.
     * @return
     */
    public List<CSVRecord> next(int rows, boolean wrap) {
        List<CSVRecord> records = new ArrayList<>(rows);
        for(int i = 0; i < rows; i++) {
            records.add(get(index++, wrap));
        }
        return records;
    }

}
