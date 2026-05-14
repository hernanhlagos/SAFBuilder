package safbuilder.util;

import java.util.Comparator;

/**
 * Alphanum comparator — sorts strings containing numbers in natural order.
 *
 * <p>Example: item_2 &lt; item_10 (whereas lexicographic order would give
 * item_10 &lt; item_2).</p>
 *
 * <p>Original implementation by David Koelle. Moved to the {@code util}
 * package and modernized for Java 17.</p>
 */
public final class AlphanumComparator implements Comparator<String> {

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private String getChunk(String s, int slength, int marker) {
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(marker);
        chunk.append(c);
        marker++;
        if (isDigit(c)) {
            while (marker < slength) {
                c = s.charAt(marker);
                if (!isDigit(c)) break;
                chunk.append(c);
                marker++;
            }
        } else {
            while (marker < slength) {
                c = s.charAt(marker);
                if (isDigit(c)) break;
                chunk.append(c);
                marker++;
            }
        }
        return chunk.toString();
    }

    @Override
    public int compare(String s1, String s2) {
        int thisMarker  = 0;
        int thatMarker  = 0;
        int s1Length    = s1.length();
        int s2Length    = s2.length();

        while (thisMarker < s1Length && thatMarker < s2Length) {
            String thisChunk = getChunk(s1, s1Length, thisMarker);
            thisMarker += thisChunk.length();
            String thatChunk = getChunk(s2, s2Length, thatMarker);
            thatMarker += thatChunk.length();

            int result;
            if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
                // Numeric comparison
                int thisChunkLen = thisChunk.length();
                result = thisChunkLen - thatChunk.length();
                if (result == 0) {
                    for (int i = 0; i < thisChunkLen; i++) {
                        result = thisChunk.charAt(i) - thatChunk.charAt(i);
                        if (result != 0) return result;
                    }
                }
            } else {
                result = thisChunk.compareTo(thatChunk);
            }
            if (result != 0) return result;
        }
        return s1Length - s2Length;
    }
}
