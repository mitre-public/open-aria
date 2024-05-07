package org.mitre.openaria.core.formats.nop;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.mitre.caasd.commons.fileutil.FileLineIterator;
import org.mitre.caasd.commons.fileutil.FileUtils;


/**
 * A NopParser "walks" through a File or other Readable resource and provides NopMessages as it
 * goes. NopParsers do not keep the entire contents of a file/stream in memory at any one time. This
 * can be very important when processing a data source that either contains an extremely large
 * amount of data or perhaps has no end whatsoever.
 */
public class NopParser implements Iterator<NopMessage>, AutoCloseable {

    private final FileLineIterator lineIter;
    private NopMessage next;

    private int lineCount;
    private int exceptionCount;

    public NopParser(File rawFile) {

        checkNotNull(rawFile, "A NopParser's input file cannot be null");
        checkArgument(rawFile.exists(), "Input file (" + rawFile.getName() + ") does not exist");
        checkArgument(!rawFile.isDirectory(), "Input file (" + rawFile.getName() + ") should not be a directory");

        this.lineIter = new FileLineIterator(silentlyGetReader(rawFile));
        this.next = findNext();
    }

    public NopParser(Reader reader) {

        checkNotNull(reader, "A NopParers's input reader cannot be null");

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

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public NopMessage next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        NopMessage returnMe = next;
        next = findNext();
        return returnMe;
    }

    private NopMessage findNext() {

        /*
         * Use a while loop based implementation because recursion can produce a StackOverflow when
         * parsing a file full on unparseable content
         * */
        while (lineIter.hasNext()) {

            lineCount++;

            try {
                return NopMessageType.parse(lineIter.next());
            } catch (NopParsingException npe) {
                this.exceptionCount++;
                //DO NOT call "return findNext();" here!
            }
        }
        return null;
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
}
