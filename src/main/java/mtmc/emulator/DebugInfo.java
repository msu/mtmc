package mtmc.emulator;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record DebugInfo(List<String> debugStrings,
                        String assemblyFile, String assemblySource, int[] assemblyLineNumbers,
                        String originalFile, int[] originalLineNumbers,
                        GlobalInfo[] globals, LocalInfo[][] locals) {

    public void handleDebugString(short debugIndex, MonTanaMiniComputer monTanaMiniComputer) {
        String debugString = debugStrings.get(debugIndex);
        Pattern compile = Pattern.compile("(\\$[a-zA-Z][a-zA-Z0-9])");
        Matcher matcher = compile.matcher(debugString);
        StringBuilder formattedString = new StringBuilder();
        int start = 0;
        int end;
        while (matcher.find()) {
            String match = matcher.group().substring(1);
            try {
                end = matcher.start();
                formattedString.append(debugString, start, end);
                Register register = Register.valueOf(match.toUpperCase());
                formattedString.append(monTanaMiniComputer.getRegisterValue(register));
                start = matcher.end();
            } catch (Exception e) {
                formattedString.append(match);
            }
        }
        formattedString.append(debugString.substring(start));
        System.out.println("DEBUG[" + System.nanoTime() + "] : " + formattedString);
    }

    public record GlobalInfo(String name, int location, String type){}
    public record LocalInfo(String name, int offset, String type){}

}