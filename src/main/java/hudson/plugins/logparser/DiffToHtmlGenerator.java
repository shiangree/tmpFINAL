package hudson.plugins.logparser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import difflib.ChangeDelta;
import difflib.DeleteDelta;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.InsertDelta;
import difflib.Patch;

/**
 * This class can generate a html page based on line diff result
 */

public class DiffToHtmlGenerator {

    /**
     * line diff results
     */
    private List<Delta> deltas = null;

    /**
     * previous and current console output text
     */
    private List<String> prevText = null;
    private List<String> currText = null;

    /**
     * generated html string
     */
    private String htmlString = null;

    /**
     * previous and current build number
     */
    private int curr;
    private int prev;

    /**
     * four different tags for unchanged, inserted, deleted and changed text
     * blocks.
     */

    private static final int UNCHANGED = 0;
    private static final int INSERTED = 1;
    private static final int DELETED = 2;
    private static final int CHANGED = 3;

    /**
     * @return deltas
     */

    public List<Delta> getDeltas() {
        return deltas;
    }

    /**
     * @return htmlString
     */

    public String getHtmlString() {
        return htmlString;
    }

    /**
     * create new DiffToHtmlGenerator object
     */
    public DiffToHtmlGenerator() {
        this(new ArrayList<String>(), new ArrayList<String>());
    }

    /**
     * create new DiffToHtmlGenerator object using two lists of strings
     *
     * @param prevText
     * @param currText
     */

    public DiffToHtmlGenerator(List<String> prevText, List<String> currText) {
        this.prevText = prevText;
        this.currText = currText;
        Patch patch = DiffUtils.diff(prevText, currText);
        deltas = patch.getDeltas();
    }

    /**
     * 
     * create new DiffToHtmlGenerator object, get two console output files, diff
     * them, and generate a html page
     * 
     * @param prevPath
     *            previous console output path
     * @param currPath
     *            current console output path
     * @param prevNum
     *            previous build number
     * @param currNum
     *            current build number
     * @throws IOException
     */

    public DiffToHtmlGenerator(String prevPath, String currPath, int prevNum, int currNum) throws IOException {
        BufferedReader prevReader = new BufferedReader(new FileReader(prevPath));
        prevText = new ArrayList<String>();
        String line = "";
        while ((line = prevReader.readLine()) != null) {
            prevText.add(line);
        }
        prevReader.close();

        BufferedReader currReader = new BufferedReader(new FileReader(currPath));
        currText = new ArrayList<String>();
        while ((line = currReader.readLine()) != null) {
            currText.add(line);
        }
        currReader.close();

        curr = currNum;
        prev = prevNum;

        Patch patch = DiffUtils.diff(prevText, currText);
        deltas = patch.getDeltas();
    }

    /**
     * generate a html string based on deltas
     *
     * @return a html string
     */
    public String generateHtmlString() {
        return generateHtmlString(false);
    }

    /**
     * generate a html string based on deltas
     *
     * @param bodyOnly only generate the body html
     * @return a html string
     * @throws Exception 
     */

    public String generateHtmlString(boolean bodyOnly) {
        StringBuilder res = new StringBuilder();

        if (!bodyOnly) {
            res.append("<!doctype html>\n");
            res.append("<html lang='en'>\n");
            res.append(generateHead());
            res.append("<body>\n");
        }

        res.append("<div style='margin: 0 auto;'>\n");
        res.append("<div class='d2h-wrapper'>\n");
        res.append("<div class='d2h-file-wrapper'>\n");
        res.append("<div class='d2h-file-header'>\n");

        if (!bodyOnly) {
            res.append("<div class='d2h-file-name'>line diff between build" + prev + " and build" + curr + ' ' 
            		+ DiffToHtmlUtils.generateButton("0","Show/Hide") + "</div>\n");
        }

        res.append("</div>\n");
        res.append("<div class='d2h-files-diff'>\n");
        res.append("<div class='d2h-file-side-diff'>\n");
        res.append("<div class='d2h-code-wrapper'>\n");
        res.append("<table class='d2h-diff-table'>\n");
        res.append("<tbody class='d2h-diff-tbody'>\n");

        // generate table based on diff result (deltas)

        StringBuilder prev = new StringBuilder();
        StringBuilder curr = new StringBuilder();

        generateHtmlFromDeltas(prev, curr);

        res.append(prev.toString());
        res.append("</tbody>\n</table>\n</div>\n</div>\n");

        res.append("<div class='d2h-file-side-diff'>\n");
        res.append("<div class='d2h-code-wrapper'>\n");
        res.append("<table class='d2h-diff-table'>\n");
        res.append("<tbody class='d2h-diff-tbody'>\n");

        res.append(curr.toString());
        res.append("</tbody>\n</table>\n</div>\n</div>\n</div>\n</div>\n</div>\n</div>\n");

        if (!bodyOnly) {
            res.append("</body>\n" + "</html>\n");
        }

        htmlString = res.toString();
        return htmlString;
    }

    /**
     * 
     * generate left and right column using deltas
     * 
     * @param prev
     *            stores left column
     * @param curr
     *            stores right column
     */

    private void generateHtmlFromDeltas(StringBuilder prev, StringBuilder curr) {
        int last = -1;
        int prevIndex = 0, currIndex = 0;
        int prevNumOfRows = 0, currNumOfRows = 0;

        for (Delta delta : deltas) {

            // append unchanged text

            if (last + 1 < delta.getOriginal().getPosition()) {

                for (int i = last + 1; i < delta.getOriginal().getPosition(); i++) {
                    prev.append(generateTableRow(prevText.get(i), prevIndex, UNCHANGED));
                    curr.append(generateTableRow(prevText.get(i), currIndex, UNCHANGED));
                    prevIndex++;
                    currIndex++;
                    prevNumOfRows++;
                    currNumOfRows++;
                }

            }

            // append changed/inserted/deleted text

            List<?> prevBlock = delta.getOriginal().getLines();
            List<?> currBlock = delta.getRevised().getLines();

            int typeNum = 0;

            if (delta instanceof InsertDelta) {
                typeNum = INSERTED;
            } else if (delta instanceof DeleteDelta) {
                typeNum = DELETED;
            } else if (delta instanceof ChangeDelta) {
                typeNum = CHANGED;
            }

            for (Object obj : prevBlock) {
                prev.append(generateTableRow(obj.toString(), prevIndex, typeNum));
                prevIndex++;
                prevNumOfRows++;
            }

            for (Object obj : currBlock) {
                curr.append(generateTableRow(obj.toString(), currIndex, typeNum));
                currIndex++;
                currNumOfRows++;
            }

            while (currNumOfRows < prevNumOfRows) {
                curr.append(generateTableRow("", -1, UNCHANGED));
                currNumOfRows++;
            }

            while (prevNumOfRows < currNumOfRows) {
                prev.append(generateTableRow("", -1, UNCHANGED));
                prevNumOfRows++;
            }

            last = delta.getOriginal().last();
        }

        if (last + 1 < prevText.size()) {
            for (int i = last + 1; i < prevText.size(); i++) {
                curr.append(generateTableRow(prevText.get(i), currIndex, UNCHANGED));
                prev.append(generateTableRow(prevText.get(i), prevIndex, UNCHANGED));
                currIndex++;
                prevIndex++;
            }
        }
    }

    /**
     * 
     * generate a
     * <tr>
     * element based on the content
     * 
     * @param s
     *            content in the row
     * @param index
     *            row number
     * @param typeNum
     *            unchanged/changed/deleted/inserted
     * @return a
     *         <tr>
     *         tag
     */

    private String generateTableRow(String s, int index, int typeNum) {
        String type = "";
        String indexString = index >= 0 ? String.valueOf(index) : "";

        switch (typeNum) {
            case UNCHANGED:
                type = "cntx";
                break;
            case INSERTED:
                type = "ins";
                break;
            case DELETED:
                type = "del";
                break;
            case CHANGED:
                type = "rev";
                break;
            default:
                break;
        }

        String res = "" + "<tr>\n" + "<td class='d2h-code-side-linenumber d2h-" + type + "'>" + indexString + "</td>\n"
                + "<td class='d2h-" + type + "'>\n" + "<div class='d2h-code-side-line d2h-" + type + "' id = 'check-"
                + type + "'><span class='d2h-code-line-ctn hljs'>" + s + "</span></div>\n" + "</td>\n" + "</tr>\n";
        return res;
    }

    private String generateHead() {
        String res = "<head>\n" + "<meta charset='utf-8'>\n" + "<title>line diff between build" + prev + "and build "
                + curr + "</title>\n" + "<link rel='stylesheet' type='text/css' href='style.css'>\n" + "<style>"
                + generateCSS() + "</style>" + DiffToHtmlUtils.generateJS("0", "d2h-cntx", ".") +"</head>\n";

        return res;
    }

    /**
     * @return a compressed css string
     */

    public String generateCSS() {
        String res = ".d2h-wrapper{display:block;margin:0 auto;"
                + "text-align:left;width:100%}.d2h-file-wrapper{border:1px "
                + "solid #ddd;border-radius:3px;margin-bottom:1em}"
                + ".d2h-file-header{padding:5px 10px;border-bottom:1px "
                + "solid #d8d8d8;background-color:#f7f7f7;font:13px " + "Helvetica,arial,freesans,clean,sans-serif,"
                + "'Segoe UI Emoji','Segoe UI Symbol'}" + ".d2h-file-name{display:inline;height:33px;"
                + "line-height:33px;max-width:80%;white-space:nowrap;" + "text-overflow:ellipsis;overflow:hidden}"
                + ".d2h-diff-table{border-collapse:collapse;" + "font-family:Consolas,'Liberation Mono',Menlo,"
                + "Courier,monospace;font-size:12px;height:18px;"
                + "line-height:18px;width:100%}.d2h-files-diff{width:100%}"
                + ".d2h-file-diff{overflow-x:scroll;overflow-y:hidden}" + ".d2h-file-side-diff{display:inline-block;"
                + "overflow-x:scroll;overflow-y:hidden;width:50%;"
                + "margin-right:-4px}.d2h-code-side-line{display:block;" + "white-space:pre;padding:0 10px;height:18px;"
                + "line-height:18px;margin-left:50px;color:inherit;"
                + "overflow-x:inherit;background:none}.d2h-code-line del,"
                + ".d2h-code-side-line del{display:inline-block;" + "margin-top:-1px;text-decoration:none;"
                + "background-color:#ffb6ba;border-radius:.2em}" + ".d2h-code-line ins,.d2h-code-side-line ins"
                + "{display:inline-block;margin-top:-1px;text-decoration:"
                + "none;background-color:#97f295;border-radius:.2em}" + ".d2h-code-side-linenumber{position:absolute;"
                + "width:35px;padding-left:10px;padding-right:10px;"
                + "height:18px;line-height:18px;background-color:#fff;"
                + "color:rgba(0,0,0,0.3);text-align:right;border:solid #eee;"
                + "border-width:0 1px;cursor:pointer;overflow:hidden;"
                + "text-overflow:ellipsis}.d2h-del{background-color:#fee8e9;"
                + "border-color:#e9aeae}.d2h-ins{background-color:#dfd;"
                + "border-color:#b4e2b4}.d2h-rev{background-color:#A9F5F2;" + "border-color:#00BFFF}";

        return res;
    }

    /**
     * 
     * write a string into a file
     * 
     * @param content
     *            a string that will be written into a file
     * @param savePath
     *            file path
     */

    public void writeToFile(String content, String savePath) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(savePath, "UTF-8");
            writer.println(content);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
