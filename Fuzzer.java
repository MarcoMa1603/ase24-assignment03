import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class Fuzzer {
    
    private static final Random random = new Random();
    private static int totalTestsRun = 0;
    private static int failedTests = 0;
    private final static Set<String> uniqueErrors = new HashSet<>();
    
    private static final String[] VALID_HTML5_TAGS = {
        "div", "span", "p", "section", "article", "nav", "header", "footer",
        "main", "aside", "figure", "figcaption", "mark", "time", "progress"
    };
    
    private static final String[] VALID_ATTRIBUTES = {
        "id", "class", "style", "title", "lang", "dir", "tabindex",
        "data-test", "role", "aria-label"
    };

    private static final List<String> SEED_INPUTS = Arrays.asList(
        "<html><head><title>Test</title></head><body><p>Hello World</p></body></html>",
        "<html><head><title>Form</title></head><body><div><p>Content</p></div></body></html>",
        "<html><head><title>Test</title></head><body><section><article>Text</article></section></body></html>",
        "<html><head><title>Test</title></head><body><p>Hello World</p></body></html>",
        
        """
        <html>
            <head><title>Form</title></head>
            <body>
                <form action="/submit" method="post" id="test-form">
                    <input type="text" name="username" required/>
                    <input type="password" name="pwd" minlength="8"/>
                    <button type="submit">Send</button>
                </form>
            </body>
        </html>
        """,
        """
        <html>
            <body>
                <!-- Main content -->
                <div class="wrapper">
                    <div class="content">
                        <ul>
                            <li>Item 1</li>
                            <li>Item 2
                                <ul>
                                    <li>Subitem</li>
                                </ul>
                            </li>
                        </ul>
                    </div>
                </div>
            </body>
        </html>
        """,
        """
        <html>
            <body>
                <div>Special chars: &lt; &gt; &amp; &quot; &apos;</div>
                <div>Symbols: © ® ™ € £ ¥</div>
                <div>Unicode: 你好 Привет مرحبا</div>
            </body>
        </html>
        """,
        """
        <html>
            <head>
                <meta charset="utf-8"/>
                <link rel="stylesheet" href="style.css"/>
            </head>
            <body>
                <img src="test.jpg" alt="Test"/>
                <br/><hr/>
                <input type="text"/>
            </body>
        </html>
        """/*,
        """
        <html>
            <body>
                <script type="text/javascript">
                    if (x < y) { return true; }
                </script>
                <style>
                    body { color: #333; }
                </style>
            </body>
        </html>
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="description" content="Example metadata.">
            <meta name="keywords" content="HTML, example, test">
            <meta name="author" content="Your Name">
            <link rel="stylesheet" href="style.css">
            <title>Metadata Example</title>
        </head>
        <body>
            <p>Metadata example in the head section.</p>
        </body>
        </html>        
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <header>
                <h1>Welcome to My Website</h1>
                <nav>
                    <a href="#about">About</a> | <a href="#contact">Contact</a>
                </nav>
            </header>
            <main>
                <section id="about">
                    <h2>About Us</h2>
                    <article>
                        <h3>Our Mission</h3>
                        <p><em>Making the world a better place</em> through code.</p>
                    </article>
                </section>
                <aside>
                    <p>Did you know? HTML5 introduced many new elements!</p>
                </aside>
            </main>
            <footer>
                <p>&copy; 2024 My Website</p>
            </footer>
        </body>
        </html>    
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <p>This is a <strong>bold</strong> and <em>italic</em> example.</p>
            <p>Here’s a <abbr title="HyperText Markup Language">HTML</abbr> abbreviation.</p>
            <p>Special text: <mark>highlighted</mark>, <sub>subscript</sub>, <sup>superscript</sup>.</p>
            <blockquote cite="https://example.com">
                This is a blockquote. Someone once said, "HTML is amazing."
            </blockquote>
            <pre>Code block with whitespace preserved.</pre>
        </body>
        </html>        
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <p>This is a <strong>bold</strong> and <em>italic</em> example.</p>
            <p>Here’s a <abbr title="HyperText Markup Language">HTML</abbr> abbreviation.</p>
            <p>Special text: <mark>highlighted</mark>, <sub>subscript</sub>, <sup>superscript</sup>.</p>
            <blockquote cite="https://example.com">
                This is a blockquote. Someone once said, "HTML is amazing."
            </blockquote>
            <pre>Code block with whitespace preserved.</pre>
        </body>
        </html>    
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <ul>
                <li>Unordered list item 1</li>
                <li>Unordered list item 2</li>
            </ul>
            <ol>
                <li>Ordered list item 1</li>
                <li>Ordered list item 2</li>
            </ol>
            <dl>
                <dt>HTML</dt>
                <dd>A markup language for the web.</dd>
                <dt>CSS</dt>
                <dd>Stylesheet language for design.</dd>
            </dl>
        </body>
        </html>
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <form action="/submit" method="post">
                <label for="name">Name:</label>
                <input type="text" id="name" name="name" required>
                <label for="email">Email:</label>
                <input type="email" id="email" name="email">
                <label for="age">Age:</label>
                <input type="number" id="age" name="age" min="0" max="120">
                <input type="submit" value="Submit">
            </form>
        </body>
        </html>
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <figure>
                <img src="image.jpg" alt="An example image">
                <figcaption>This is a caption for the image.</figcaption>
            </figure>
            <audio controls>
                <source src="audio.mp3" type="audio/mpeg">
                Your browser does not support the audio element.
            </audio>
            <video controls>
                <source src="video.mp4" type="video/mp4">
                Your browser does not support the video element.
            </video>
        </body>
        </html>
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <table>
                <caption>Sample Table</caption>
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Age</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Alice</td>
                        <td>30</td>
                    </tr>
                    <tr>
                        <td>Bob</td>
                        <td>25</td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr>
                        <td colspan="2">End of Table</td>
                    </tr>
                </tfoot>
            </table>
        </body>
        </html>
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <iframe src="https://example.com" title="Example iframe"></iframe>
            <embed src="document.pdf" type="application/pdf">
            <object data="image.svg" type="image/svg+xml">
                <p>Fallback content if object is not supported.</p>
            </object>
        </body>
        </html>        
        """,
        """
        <!DOCTYPE html>
        <html lang="en">
        <body>
            <details>
                <summary>Click to reveal more info</summary>
                <p>This is hidden content that appears on click.</p>
            </details>
            <progress value="50" max="100">50%</progress>
            <meter value="0.7" min="0" max="1">70%</meter>
        </body>
        </html>
        """,
        """
        <!DOCTYPE html>
        <html>
        <head><title>Details Test</title></head>
        <body>
            <details>
                <summary>Click to show details</summary>
                <p>Hidden content here.</p>
            </details>
        </body>
        </html>
        """,
        """
        <!DOCTYPE html>
        <html>
        <head><title>Progress Test</title></head>
        <body>
            <progress value="70" max="100">70%</progress>
        </body>
        </html>
        """,
        """
        <!DOCTYPE html>
        <html>
        <head><title>Meter Test</title></head>
        <body>
            <meter value="0.6" min="0" max="1">60%</meter>
        </body>
        </html>
        """,
        // Really complex html with almost every tag
        """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="description" content="A complex HTML example showcasing every HTML5 tag.">
                    <meta http-equiv="X-UA-Compatible" content="IE=edge">
                    <link rel="stylesheet" href="styles.css">
                    <title>Complex HTML Example</title>
                </head>
                <body>
                    <header>
                        <nav>
                            <ul>
                                <li><a href="#main">Main Content</a></li>
                                <li><a href="#aside">Sidebar</a></li>
                                <li><a href="#footer">Footer</a></li>
                            </ul>
                        </nav>
                    </header>
                    <main id="main">
                        <section>
                            <article>
                                <header>
                                    <h1>Article Title</h1>
                                    <time datetime="2024-11-29">November 29, 2024</time>
                                </header>
                                <p>This is an example paragraph with some <mark>highlighted text</mark>, <abbr title="HyperText Markup Language">HTML</abbr>, and a <cite>citation</cite>.</p>
                                <figure>
                                    <img src="image.jpg" alt="An example image">
                                    <figcaption>An image with a caption.</figcaption>
                                </figure>
                                <details>
                                    <summary>Click to expand details</summary>
                                    <p>Here are some hidden details revealed on click.</p>
                                </details>
                                <footer>
                                    <p>Written by <address>Author Name, <a href="mailto:author@example.com">author@example.com</a></address></p>
                                </footer>
                            </article>
                        </section>
                        <section>
                            <h2>Embedded Content</h2>
                            <iframe src="https://example.com" title="Example iframe"></iframe>
                            <audio controls>
                                <source src="audio.mp3" type="audio/mpeg">
                                Your browser does not support the audio element.
                            </audio>
                            <video controls>
                                <source src="video.mp4" type="video/mp4">
                                Your browser does not support the video element.
                            </video>
                            <embed src="document.pdf" type="application/pdf">
                        </section>
                        <section>
                            <h2>Interactive Elements</h2>
                            <form action="/submit" method="post">
                                <fieldset>
                                    <legend>Personal Information</legend>
                                    <label for="name">Name:</label>
                                    <input type="text" id="name" name="name" required>
                                    <label for="email">Email:</label>
                                    <input type="email" id="email" name="email">
                                    <label for="birthday">Birthday:</label>
                                    <input type="date" id="birthday" name="birthday">
                                    <label for="color">Favorite Color:</label>
                                    <input type="color" id="color" name="color">
                                </fieldset>
                                <fieldset>
                                    <legend>Preferences</legend>
                                    <label for="file">Upload File:</label>
                                    <input type="file" id="file" name="file">
                                    <label for="range">Volume:</label>
                                    <input type="range" id="range" name="range" min="0" max="10">
                                    <label for="number">Count:</label>
                                    <input type="number" id="number" name="number" min="1" max="5">
                                </fieldset>
                                <button type="submit">Submit</button>
                            </form>
                        </section>
                    </main>
                    <aside id="aside">
                        <h2>Sidebar</h2>
                        <menu>
                            <menuitem label="Home"></menuitem>
                            <menuitem label="Settings"></menuitem>
                            <menuitem label="Help"></menuitem>
                        </menu>
                    </aside>
                    <footer id="footer">
                        <h2>Footer Content</h2>
                        <address>
                            Contact us at <a href="mailto:info@example.com">info@example.com</a>
                        </address>
                        <output>Calculated value: 42</output>
                        <progress value="70" max="100">70%</progress>
                        <meter value="0.6">60%</meter>
                    </footer>
                    <script>
                        console.log('Complex HTML example loaded.');
                    </script>
                </body>
                </html>
                """*/
    
    );

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Fuzzer.java \"<command_to_fuzz>\"");
            System.exit(1);
        }
    
        String commandToFuzz = args[0];
        String workingDirectory = "./";
    
        if (!Files.exists(Paths.get(workingDirectory, commandToFuzz))) {
            throw new RuntimeException("Could not find command '%s'.".formatted(commandToFuzz));
        }
    
        ProcessBuilder builder = getProcessBuilderForCommand(commandToFuzz, workingDirectory);
        
        List<Function<String, String>> mutators = Arrays.asList(
            input -> addValidElement(input),
            input -> addDeepValidNesting(input),
            input -> addLongValidContent(input),
            input -> addValidAttributes(input),
            input -> addComplexUnicode(input),
            input -> modifyValidStructure(input)
        );

        System.out.println("\n=== Phase 1: Running all seeds without backtracking ===");
        for (int seedIndex = 0; seedIndex < SEED_INPUTS.size(); seedIndex++) {
            String seed = SEED_INPUTS.get(seedIndex);
            System.out.printf("\nTesting seed %d/%d without backtracking:\n", seedIndex + 1, SEED_INPUTS.size());
            List<String> mutations = getMutatedInputsWithoutBacktracking(seed, mutators);
            testWithoutBacktracking(builder, seed, mutations);
        }
    
        System.out.println("\n=== Phase 2: Running all seeds with backtracking ===");
        for (int seedIndex = 0; seedIndex < SEED_INPUTS.size(); seedIndex++) {
            String seed = SEED_INPUTS.get(seedIndex);
            System.out.printf("\nTesting seed %d/%d with backtracking:\n", seedIndex + 1, SEED_INPUTS.size());
            testWithBacktracking( seed, mutators, commandToFuzz);
        }
    
        
        printSummary();
        
        if (failedTests > 0) {
            System.exit(1);
        }
    }

    private static void getMutatedInputsWithBacktracking(String seedInput, Collection<Function<String, String>> mutators, String commandToFuzz) {
        ProcessBuilder testBuilder = getProcessBuilderForCommand(commandToFuzz, "./");
        String lastSuccessfulMutation = seedInput;
        
        for (int i = 0; i < 50; i++) {
            System.out.println("\nMutation " + (i+1) + "/50:");
            String currentInput = lastSuccessfulMutation;  // Start from last successful state
            
            // Apply a single mutation
            Function<String, String> mutator = new ArrayList<>(mutators)
                .get(random.nextInt(mutators.size()));
            currentInput = mutator.apply(currentInput);
            totalTestsRun++;
            
            try {

                Process process = testBuilder.start();
                try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
                    writer.write(currentInput);
                    writer.flush();
                }
                
                int result = process.waitFor();
                
                if (result != 0) {
                    failedTests++;
                    String output = readStreamIntoString(process.getInputStream());
                    System.out.println("\nFound crash with input:");
                    System.out.println(currentInput);
                    System.out.println("Exit code: " + result);
                    if (!output.isEmpty()) {
                        System.out.println("Program output: " + output);
                        uniqueErrors.add(output.trim());
                    }
                    
                    // Backtrack by keeping lastSuccessfulMutation unchanged
                } else {
                    // Update base for future mutations if this mutation was successful
                    System.out.println("\nSuccessfully built upon previous mutation:");
                    System.out.println("Previous: " + lastSuccessfulMutation);
                    System.out.println("New: " + currentInput);
                    lastSuccessfulMutation = currentInput;
                }
                
            } catch (IOException | InterruptedException e) {
                System.out.println("\nException occurred with input:");
                System.out.println(currentInput);
                e.printStackTrace();
                // Backtrack by keeping lastSuccessfulMutation unchanged
            }
        }
    }
    private static List<String> getMutatedInputsWithoutBacktracking(String seedInput, Collection<Function<String, String>> mutators) {
        List<String> mutations = new ArrayList<>();
        int numberOfMutations = 50;
        
        for (int i = 0; i < numberOfMutations; i++) {
            System.out.println("\nMutation (" + (i+1) + "/50):");
            String currentInput = seedInput;
            int numMutationsToApply = random.nextInt(3) + 1;
            
            // Apply mutations without testing
            for (int j = 0; j < numMutationsToApply; j++) {
                Function<String, String> mutator = new ArrayList<>(mutators)
                    .get(random.nextInt(mutators.size()));
                currentInput = mutator.apply(currentInput);
            }
            
            // Add mutation if it's different from seed
            if (!currentInput.equals(seedInput)) {
                mutations.add(currentInput);
            }
        }
        
        return mutations;
    }
    private static void printSummary() {
        System.out.println("\nUnique Error Messages Found:");
        uniqueErrors.forEach(error -> System.out.println("- " + error));

        System.out.println("\nFinal Results:");
        System.out.println("Total tests run: " + totalTestsRun);
        System.out.println("Failed tests: " + failedTests);
        System.out.println("Unique errors found: " + uniqueErrors.size());
    }

    private static String addValidElement(String input) {
        String tag = VALID_HTML5_TAGS[random.nextInt(VALID_HTML5_TAGS.length)];
        String newElement = String.format("<%s>Valid Content</%s>", tag, tag);
        System.out.println(" addValidElement");
        return insertIntoBody(input, newElement);
    }

    private static String addDeepValidNesting(String input) {
        int depth = random.nextInt(10) + 5; // 5-15 levels
        StringBuilder nested = new StringBuilder();
        Stack<String> tags = new Stack<>();
        
        for (int i = 0; i < depth; i++) {
            String tag = VALID_HTML5_TAGS[random.nextInt(VALID_HTML5_TAGS.length)];
            nested.append("<").append(tag).append(">");
            tags.push(tag);
        }
        
        nested.append("Deep nested content");
        
        while (!tags.isEmpty()) {
            nested.append("</").append(tags.pop()).append(">");
        }
        System.out.println(" addDeepValidNesting");
        return insertIntoBody(input, nested.toString());
    }

    private static String addLongValidContent(String input) {
        int length = random.nextInt(100) + 50;
        StringBuilder content = new StringBuilder();
        String[] words = {"lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit"};
        
        for (int i = 0; i < length; i++) {
            content.append(words[random.nextInt(words.length)]).append(" ");
        }
        
        System.out.println(" addLongValidContent");
        return insertIntoBody(input, "<div>" + content + "</div>");
    }

    private static String addValidAttributes(String input) {
        String tag = VALID_HTML5_TAGS[random.nextInt(VALID_HTML5_TAGS.length)];
        StringBuilder attrs = new StringBuilder();
        
        int numAttrs = random.nextInt(5) + 1;
        for (int i = 0; i < numAttrs; i++) {
            String attr = VALID_ATTRIBUTES[random.nextInt(VALID_ATTRIBUTES.length)];
            String value = generateValidAttributeValue();
            attrs.append(" ").append(attr).append("=\"").append(value).append("\"");
        }
        
        String element = String.format("<%s%s>Valid Content</%s>", tag, attrs, tag);
        System.out.println(" addValidAttributes");
        return insertIntoBody(input, element);
    }

    private static String addComplexUnicode(String input) {
        String[] validUnicode = {
            "Hello 你好 สวัสดี नमस्ते", 
            "Multiple Scripts العربية עברית",
            "Emojis 👋 🌍 🌟 ♥️"
        };
        
        String content = validUnicode[random.nextInt(validUnicode.length)];
        System.out.println(" addComplexUnicode");
        return insertIntoBody(input, "<div>" + content + "</div>");
    }

    private static String modifyValidStructure(String input) {
        // Adds valid structural elements while maintaining HTML validity
        String[] structures = {
            "<header><h1>Header</h1></header>",
            "<nav><ul><li>Nav Item</li></ul></nav>",
            "<main><article><section>Content</section></article></main>",
            "<footer><p>Footer</p></footer>"
        };
        System.out.println("modifyValidStructure");
        return insertIntoBody(input, structures[random.nextInt(structures.length)]);
    }

    private static String generateValidAttributeValue() {
        int length = random.nextInt(50) + 10;
        StringBuilder value = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";
        
        for (int i = 0; i < length; i++) {
            value.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return value.toString();
    }

    private static String insertIntoBody(String input, String content) {
        int bodyIndex = input.toLowerCase().indexOf("<body>");
        if (bodyIndex == -1) return input;
        
        int insertIndex = bodyIndex + "<body>".length();
        return input.substring(0, insertIndex) + content + input.substring(insertIndex);
    }

    private static ProcessBuilder getProcessBuilderForCommand(String command, String workingDirectory) {
        ProcessBuilder builder = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", "./" + command);
        }
        builder.directory(new File(workingDirectory));
        builder.redirectErrorStream(true);
        return builder;
    }

    
    private static boolean testInput(ProcessBuilder builder, String input) {
        try {
            Process process = builder.start();
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
                writer.write(input);
            }
            
            int result = process.waitFor();
            totalTestsRun++;
            
            if (result != 0) {
                failedTests++;
                System.out.println("\nFound crash with input:");
                System.out.println(input);
                System.out.println("Exit code: " + result);
                
                String output = readStreamIntoString(process.getInputStream());
                if (!output.isEmpty()) {
                    System.out.println("Program output: " + output);
                    uniqueErrors.add(output.trim());
                }
                return true; // Error found
            }
            return false; // No error
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return true; // Consider exceptions as errors
        }
    }

    private static String readStreamIntoString(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines()
                .map(line -> line + System.lineSeparator())
                .collect(Collectors.joining());
    }

    private static void testWithBacktracking(String seed, Collection<Function<String, String>> mutators, String commandToFuzz) {
        System.out.println("Generating and testing mutations with backtracking...");
        getMutatedInputsWithBacktracking(seed, mutators, commandToFuzz);
        System.out.println();
    }
    
    private static void testWithoutBacktracking(ProcessBuilder builder, String seed, List<String> mutations) {
        System.out.println("Testing original seed input...");
        boolean test_has_error = testInput(builder, seed);
        if(test_has_error)
            return;
        int totalMutations = mutations.size();
        for (int i = 0; i < mutations.size(); i++) {
            String input = mutations.get(i);
            System.out.printf("\rTesting mutation %d/%d (Failed: %d)...", 
                i + 1, totalMutations, failedTests);
            test_has_error = testInput(builder, input);
            
            if (test_has_error) {
                break;  // Stop testing mutations after first error in non-backtracking mode
            }
        }
        System.out.println();
    }

}