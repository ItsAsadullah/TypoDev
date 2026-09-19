package juloo.keyboard2.suggestions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Developer Syntax & Coding Auto-Suggestion Engine.
 * Supports HTML/XML/TSX tags, closing tags, and language keywords/snippets
 * for Python, JavaScript, TypeScript, Java, HTML, and CSS.
 */
public class DevSyntaxEngine
{
  private static DevSyntaxEngine _instance;

  public static synchronized DevSyntaxEngine instance()
  {
    if (_instance == null)
    {
      _instance = new DevSyntaxEngine();
    }
    return _instance;
  }

  public static class TagInfo
  {
    public final String tag;
    public final boolean isSelfClosing;

    public TagInfo(String tag, boolean isSelfClosing)
    {
      this.tag = tag;
      this.isSelfClosing = isSelfClosing;
    }
  }

  private final List<TagInfo> _htmlTags = new ArrayList<>();
  private final List<String> _codeKeywords = new ArrayList<>();

  private DevSyntaxEngine()
  {
    initHtmlTags();
    initCodeKeywords();
  }

  private void initHtmlTags()
  {
    // Container & Layout Tags
    addTag("div", false);
    addTag("span", false);
    addTag("p", false);
    addTag("a", false);
    addTag("button", false);
    addTag("dialog", false);
    addTag("details", false);
    addTag("summary", false);
    addTag("input", true);
    addTag("img", true);
    addTag("link", true);
    addTag("meta", true);
    addTag("hr", true);
    addTag("br", true);

    // Document & Semantic Structure
    addTag("html", false);
    addTag("head", false);
    addTag("body", false);
    addTag("header", false);
    addTag("footer", false);
    addTag("section", false);
    addTag("article", false);
    addTag("main", false);
    addTag("nav", false);
    addTag("aside", false);

    // Lists & Tables
    addTag("ul", false);
    addTag("ol", false);
    addTag("li", false);
    addTag("table", false);
    addTag("tr", false);
    addTag("td", false);
    addTag("th", false);
    addTag("tbody", false);
    addTag("thead", false);

    // Forms & Inputs
    addTag("form", false);
    addTag("label", false);
    addTag("select", false);
    addTag("option", false);
    addTag("textarea", false);

    // Scripts & Media
    addTag("script", false);
    addTag("style", false);
    addTag("iframe", false);
    addTag("canvas", false);
    addTag("svg", false);
    addTag("path", true);
    addTag("video", false);
    addTag("audio", false);

    // Typography & Headings
    addTag("h1", false);
    addTag("h2", false);
    addTag("h3", false);
    addTag("h4", false);
    addTag("h5", false);
    addTag("h6", false);
    addTag("strong", false);
    addTag("em", false);
    addTag("code", false);
    addTag("pre", false);
  }

  private void addTag(String tag, boolean isSelfClosing)
  {
    _htmlTags.add(new TagInfo(tag, isSelfClosing));
  }

  private void initCodeKeywords()
  {
    // JavaScript / TypeScript / TSX / React
    addCode("console.log()", "console.error()", "console.warn()", "console.table()");
    addCode("const", "let", "var", "function", "return", "import", "export", "export default", "export const");
    addCode("interface", "type", "async", "await", "Promise");
    addCode("useState()", "useEffect()", "useCallback()", "useMemo()", "useRef()", "useContext()");
    addCode("document.getElementById()", "addEventListener()", "querySelector()", "querySelectorAll()");
    addCode("JSON.stringify()", "JSON.parse()");
    addCode("className=\"\"", "onClick={() => {}}", "onChange={(e) => {}}", "onSubmit={handleSubmit}");
    addCode("<Fragment></Fragment>", "<React.Fragment>", "undefined", "null", "typeof", "instanceof");

    // Python
    addCode("def", "class", "print()", "self", "__init__()", "__str__()");
    addCode("if __name__ == '__main__':", "elif", "except Exception as e:", "finally:", "raise");
    addCode("with open() as f:", "range()", "len()", "enumerate()", "isinstance()", "lambda");
    addCode("list()", "dict()", "set()", "tuple()", "append()", "super().__init__()");
    addCode("import", "from", "yield", "async def", "True", "False", "None");

    // Java
    addCode("public class", "public static void main(String[] args)", "System.out.println()");
    addCode("private", "protected", "public", "static", "final", "void", "package");
    addCode("@Override", "implements", "extends", "boolean", "String", "int", "double", "float");
    addCode("ArrayList<>", "HashMap<>", "List<>", "Map<>", "catch (Exception e)", "throws", "throw new");
    addCode("StringBuilder", "Integer", "Boolean", "Long");

    // CSS
    addCode("display: flex;", "display: grid;", "display: none;", "display: inline-block;");
    addCode("position: absolute;", "position: relative;", "position: fixed;", "position: sticky;");
    addCode("margin: 0 auto;", "padding:", "color:", "background:", "background-color:");
    addCode("width: 100%;", "height: 100%;", "max-width:", "min-height:");
    addCode("font-size:", "font-weight: bold;", "text-align: center;", "line-height:");
    addCode("border: 1px solid ", "border-radius:", "box-shadow:");
    addCode("justify-content: center;", "align-items: center;", "flex-direction: column;");
    addCode("z-index:", "cursor: pointer;", "overflow: hidden;", "transition: all 0.3s ease;");
    addCode("flex: 1;", "gap:", "opacity:", "transform:");
  }

  private void addCode(String... items)
  {
    for (String item : items)
    {
      if (!_codeKeywords.contains(item))
      {
        _codeKeywords.add(item);
      }
    }
  }

  /**
   * Generates developer coding candidates based on current input and context.
   * Handles `<di` -> `<div>`, `</div>`, `div`, `<div></div>`,
   * `</` -> closing tags,
   * and language keywords like `def`, `console.log()`, `display: flex;`.
   */
  public List<Candidate> queryCandidates(String word, String textBeforeCursor, int maxResults)
  {
    if (maxResults <= 0) return Collections.emptyList();

    LinkedHashSet<Candidate> candidates = new LinkedHashSet<>();
    String token = (word != null) ? word.trim() : "";

    // Check if user has typed '<' or '</' before the cursor
    String prefixWithAngle = null;
    if (textBeforeCursor != null)
    {
      String trimmedBefore = textBeforeCursor.trim();
      if (trimmedBefore.endsWith("</" + token))
      {
        prefixWithAngle = "</" + token;
      }
      else if (trimmedBefore.endsWith("<" + token))
      {
        prefixWithAngle = "<" + token;
      }
      else if (trimmedBefore.endsWith("</"))
      {
        prefixWithAngle = "</";
      }
      else if (trimmedBefore.endsWith("<"))
      {
        prefixWithAngle = "<";
      }
    }

    if (token.startsWith("<"))
    {
      prefixWithAngle = token;
    }

    // 1. Tag Suggestions when '<' is typed (e.g. "<di", "<ht", "<", "</")
    if (prefixWithAngle != null)
    {
      boolean isClosing = prefixWithAngle.startsWith("</");
      String tagQuery = isClosing ? prefixWithAngle.substring(2).toLowerCase(Locale.ROOT)
                                  : prefixWithAngle.substring(1).toLowerCase(Locale.ROOT);

      for (TagInfo ti : _htmlTags)
      {
        if (tagQuery.isEmpty() || ti.tag.startsWith(tagQuery))
        {
          if (isClosing)
          {
            if (!ti.isSelfClosing)
            {
              candidates.add(new Candidate("</" + ti.tag + ">", Candidate.Source.CODE_SNIPPET, 255, 0, 1.0f));
            }
          }
          else
          {
            // E.g. for "<di": suggest "<div>", "</div>", "div", "<div></div>"
            if (ti.isSelfClosing)
            {
              candidates.add(new Candidate("<" + ti.tag + " />", Candidate.Source.CODE_SNIPPET, 255, 0, 1.0f));
              candidates.add(new Candidate(ti.tag, Candidate.Source.CODE_SNIPPET, 245, 0, 1.0f));
            }
            else
            {
              candidates.add(new Candidate("<" + ti.tag + ">", Candidate.Source.CODE_SNIPPET, 255, 0, 1.0f));
              candidates.add(new Candidate("</" + ti.tag + ">", Candidate.Source.CODE_SNIPPET, 250, 0, 1.0f));
              candidates.add(new Candidate("<" + ti.tag + "></" + ti.tag + ">", Candidate.Source.CODE_SNIPPET, 248, 0, 1.0f));
              candidates.add(new Candidate(ti.tag, Candidate.Source.CODE_SNIPPET, 245, 0, 1.0f));
            }
          }
          if (candidates.size() >= maxResults) break;
        }
      }

      if (prefixWithAngle.equals("<ht") || prefixWithAngle.equals("<html"))
      {
        candidates.add(new Candidate("<!DOCTYPE html>", Candidate.Source.CODE_SNIPPET, 250, 0, 1.0f));
      }

      return new ArrayList<>(candidates);
    }

    // 2. Tag Suggestions without '<' (e.g. user types "div", "span", "html")
    if (!token.isEmpty())
    {
      String lower = token.toLowerCase(Locale.ROOT);
      for (TagInfo ti : _htmlTags)
      {
        if (ti.tag.equals(lower) || (lower.length() >= 2 && ti.tag.startsWith(lower)))
        {
          if (ti.isSelfClosing)
          {
            candidates.add(new Candidate("<" + ti.tag + " />", Candidate.Source.CODE_SNIPPET, 242, 0, 1.0f));
          }
          else
          {
            candidates.add(new Candidate("<" + ti.tag + ">", Candidate.Source.CODE_SNIPPET, 242, 0, 1.0f));
            candidates.add(new Candidate("</" + ti.tag + ">", Candidate.Source.CODE_SNIPPET, 240, 0, 1.0f));
            candidates.add(new Candidate("<" + ti.tag + "></" + ti.tag + ">", Candidate.Source.CODE_SNIPPET, 238, 0, 1.0f));
          }
          if (candidates.size() >= maxResults) break;
        }
      }
    }

    // 3. Programming Keywords & Syntax Snippets
    if (token.length() >= 2)
    {
      String lower = token.toLowerCase(Locale.ROOT);
      for (String kw : _codeKeywords)
      {
        if (kw.toLowerCase(Locale.ROOT).startsWith(lower))
        {
          candidates.add(new Candidate(kw, Candidate.Source.CODE_SNIPPET, 244, 0, 1.0f));
          if (candidates.size() >= maxResults) break;
        }
      }
    }

    return new ArrayList<>(candidates);
  }
}
