package org.mitre.openaria.core.formats.ariacsv;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.util.DemotedException.demote;
import static org.mitre.openaria.core.formats.ariacsv.AriaCsvHits.parsePointFromAriaCsv;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.mitre.caasd.commons.fileutil.FileLineIterator;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.openaria.core.Point;


public class AriaCsvParser implements Iterator<Point<AriaCsvHit>>, AutoCloseable {

    private final FileLineIterator lineIter;
    private String next;

    private int lineCount;
    private int exceptionCount;


    /** Parse a File directly. */
    public AriaCsvParser(File rawFile) {

        checkNotNull(rawFile, "An AriaCsvParser's input file cannot be null");
        checkArgument(rawFile.exists(), "Input file (" + rawFile.getName() + ") does not exist");
        checkArgument(!rawFile.isDirectory(), "Input file (" + rawFile.getName() + ") should not be a directory");

        this.lineIter = new FileLineIterator(silentlyGetReader(rawFile));
        this.next = findNext();
    }


    /** Parse a BufferedReader directly. */
    public AriaCsvParser(Reader reader) {

        checkNotNull(reader, "An AriaCsvParser's input reader cannot be null");

        this.lineIter = new FileLineIterator(reader);
        this.next = findNext();
    }


    /*
     * Convert Checked Exceptions to Unchecked Exceptions.
     */
    private static BufferedReader silentlyGetReader(File rawFile) {

        try {
            return FileUtils.createReaderFor(rawFile);
        } catch (Exception ex) {
            //rethrow as unchecked exception to simplfied
            throw demote("Problem creating reader for: " + rawFile.getAbsolutePath(), ex);
        }
    }


    /** @return This Iterator viewed as a Stream. */
    public Stream<Point<AriaCsvHit>> stream() {
        // Intentionally avoiding Guava's Stream.stream(Iterator<T> iterator) method.
        // This method has the @Beta annotation
        // However, we are using the exact same implementation (e.g. relying on JDK code)
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, 0), false);
    }


    private String findNext() {

        /*
         * Implemented with a while loop implementation because recursion can produce a
         * StackOverflow when parsing a file full of unparse-able content
         */
        while (lineIter.hasNext()) {

            lineCount++;

            try {
                return lineIter.next();
            } catch (RuntimeException npe) {
                this.exceptionCount++;
                //DO NOT call "return findNext();" here!
            }
        }
        return null;
    }


    @Override
    public boolean hasNext() {
        return next != null;
    }


    @Override
    public Point<AriaCsvHit> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        String returnMe = next;
        next = findNext();

        return parsePointFromAriaCsv(returnMe);
    }


    /**
     * Return the total number lines this parser has attempted to process. Lines that were not
     * parsed properly will not correspond to output. Consequently, this number is NOT necessarily
     * the same as the number of times the ".next()" method was called on the iterator.
     *
     * @return The total number of lines this parser has attempted to process.
     */
    public int currentLineCount() {
        return lineCount;
    }

    public int exceptionCount() {
        return this.exceptionCount;
    }

    @Override //part of AutoCloseable
    public void close() throws Exception {

        /*
         * ensure "hasNext()" always returns false after close() is called. We want consistent
         * behavior if this Iterator is closed before it reaches the end of the file.
         */
        next = null;
        lineIter.close();
    }
}


