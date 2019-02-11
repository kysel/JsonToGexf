using CommandLine;

namespace JsonToGexf
{
    public class Options
    {
        [Option('i', "input",
            Required = true,
            HelpText = "Path to json input file")]
        public string Input { get; set; }

        [Option('o', "output",
            Required = false,
            HelpText = "Output file name, defaults to input file name.")]
        public string Out { get; set; }
    }
}