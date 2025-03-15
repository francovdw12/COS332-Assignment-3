import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class CalculatorHTTPServer{

    public static void main(String[] args) throws IOException {

        int port = 8000;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new CalculatorHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server is running on port " + port);

    }

    static class CalculatorHandler implements HttpHandler{

        private StringBuilder input = new StringBuilder();
        private boolean lastWasEvaluation = false;

        public void handle(HttpExchange exchange) throws IOException {

            try {

                String method = exchange.getRequestMethod();

                //Added this to handle HEAD request, without this, a HEAD request would be treated exactly like a GET request (response body included)
                if(method.equalsIgnoreCase("HEAD")){

                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, -1);

                    return;

                }

                String path = exchange.getRequestURI().getPath().substring(1);

                if(path.equals("favicon.ico")){

                    exchange.sendResponseHeaders(404, -1);
                    return;

                }
                
                if(!path.isEmpty()){

                    if(path.equals("=")){

                        String result = evaluateExpression(input.toString());
                        input.setLength(0);
                        input.append(result);

                        lastWasEvaluation = true;

                    } else if(path.equals("clear")){

                        input.setLength(0);

                        lastWasEvaluation = false;

                    } else{

                        if(lastWasEvaluation && isANumber(path)){

                            input.setLength(0);

                        }

                        input.append(path);

                        lastWasEvaluation = false;

                    }

                }

                String response = generateCalculatorPage(input.toString());
                byte[] responseBytes = response.getBytes("UTF-8");

                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();

            } catch(Exception e){

                System.err.println("Error handling request: " + e.getMessage());
                String errorResponse = "Internal Server Error";
                byte[] errorBytes = errorResponse.getBytes("UTF-8");
                exchange.sendResponseHeaders(500, errorBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorBytes);
                os.close();

            }

        }

        private boolean isANumber(String s){

            return s.matches("\\d");

        }


        private String generateCalculatorPage(String currentExpression){

            StringBuilder html = new StringBuilder();

            html.append("<html><body>");
            html.append("<h1>Calculator</h1>");
            html.append("<p>Expression: ").append(currentExpression).append("</p>");

            List<String> calculatorButtons = List.of("1", "2", "3", "+", "4", "5", "6", "-", "7", "8", "9", "*", "0", "=", "/", "clear");

            for(String button : calculatorButtons){

                if(button.equals("/")){ //make sure that the program handles the '/' case correctly

                    html.append("<a href=\"/%2F\">").append(button).append("</a> ");

                } else{

                    html.append("<a href=\"/").append(button).append("\">").append(button).append("</a> ");

                }

                if(button.equals("+") || button.equals("-") || button.equals("*") || button.equals("/")){

                    html.append("<br>");

                }

            }

            html.append("</body></html>");

            return html.toString();

        }

        private String evaluateExpression(String expr){

            try{

                ExpressionParser parser = new ExpressionParser(expr);
                double result = parser.parse();
                return Double.toString(result);

            } catch(Exception e){

                e.printStackTrace();
                return "Error";

            }

        }
        
        private static class ExpressionParser{

            private final String input;
            private int position = -1;
            private int ch;
        
            public ExpressionParser(String input){

                this.input = input;
                nextChar();

            }
        
            //move on one character
            private void nextChar(){

                position++;

                if(position < input.length()){

                    ch = input.charAt(position);

                } else{

                    ch = -1;

                }

            }
        
            //accept character if it matches the particular character looked for
            private boolean accept(int charToEat){

                while(ch == ' '){

                    nextChar();

                } 

                if(ch == charToEat){

                    nextChar();
                    return true;

                }

                return false;

            }
        
            //parse entire expressin
            public double parse(){

                double x = parseExpression();

                if(position < input.length()) throw new RuntimeException("Unexpected: " + (char)ch);

                return x;

            }
        
            //define grammer:
            //Handle + or - operators
            private double parseExpression(){

                double x = parseTerm();

                //create endless loop
                for(;;){

                    if(accept('+')){

                        x += parseTerm();

                    } else if(accept('-')){

                        x -= parseTerm();

                    } else{

                        return x;

                    }

                }
            }
        
            //define * and / operators
            private double parseTerm(){

                double x = parseFactor();

                //create endless loop
                for(;;){

                    if(accept('*')){

                        x *= parseFactor();
                        
                    } else if(accept('/')){

                        x /= parseFactor();

                    } else{

                        return x;

                    }

                }
            }
        
            //handle + - operators
            private double parseFactor(){

                if (accept('+')) return parseFactor();
                if (accept('-')) return -parseFactor();
        
                double x;
                int startPos = this.position;

                if(accept('(')){

                    x = parseExpression();
                    if(!accept(')')) throw new RuntimeException("Missing ')'");

                } else if((ch >= '0' && ch <= '9') || ch == '.'){

                    while ((ch >= '0' && ch <= '9') || ch == '.'){

                        nextChar();

                    } 

                    String numberStr = input.substring(startPos, position);
                    x = Double.parseDouble(numberStr);

                } else{

                    throw new RuntimeException("Unexpected character: " + (char)ch);

                }

                return x;

            }
            
        }

    }

}
