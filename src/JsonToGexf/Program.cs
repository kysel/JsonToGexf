using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using System.Xml.Serialization;
using CommandLine;
using Newtonsoft.Json;

namespace JsonToGexf
{
    class Program
    {
        static async Task<int> Main(string[] args) {
            Options opts = null;
            new Parser(cfg => cfg.HelpWriter = Console.Out)
                .ParseArguments<Options>(args)
                .WithParsed(o => opts = o);

            if (opts == null)
                return 1;

            if (!File.Exists(opts.Input)) {
                await Console.Out.WriteLineAsync("Input file does not exists");
                return 2;
            }

            if (string.IsNullOrEmpty(opts.Out)) {
                opts.Out = Path.ChangeExtension(opts.Input, ".gexf");
            }

            try {
                var cv = new Converter(opts);
                cv.Convert();
            }
            catch (Exception e) {
                Console.WriteLine("Ooops an error has encountered:");
                Console.WriteLine(e);
                return 1337;
            }

            return 0;
        }
    }

    public class Converter
    {
        private readonly Options _opts;

        public Converter(Options opts) {
            _opts = opts;
        }

        public void Convert() {
            JsonInput deserialized;
            using (var sr = File.OpenText(_opts.Input))
            using (var jr = new JsonTextReader(sr))
                deserialized = JsonSerializer.CreateDefault().Deserialize<JsonInput>(jr);


            var gc = new graphcontent();
            var nodes = new List<nodecontent>();
            var edges = new List<edgecontent>();
            for (var cIdx = 0; cIdx < deserialized.Clusters.Count; cIdx++) {
                var cluster = deserialized.Clusters[cIdx];
                foreach (var node in cluster.Nodes.Values) {
                    var attVals = new attvaluescontent {
                        attvalue = new [] {
                            new attvalue{@for = "Cluster", value = cIdx.ToString()},
                            new attvalue{@for = "descr", value = node.Descr},
                        }
                    };
                    nodes.Add(new nodecontent {
                        id = node.Id.ToString(),
                        label = node.Code,
                        Items = new object[] {attVals}
                    });
                }

                foreach (var edge in cluster.Edges) {
                    edges.Add(new edgecontent {
                        source = edge.Source.ToString(),
                        target = edge.Target.ToString(),
                        weight = edge.Weight,
                        weightSpecified = true,
                    });
                }
            }

            gc.Items = new object[] {
                new attributescontent {
                    mode = modetype.@static,
                    attribute = new [] {
                        new attributecontent{id = "Cluster", type = attrtypetype.integer, title = "Cluster"},
                        new attributecontent{id = "descr", type = attrtypetype.@string, title = "Description"},
                    }
                },
                new nodescontent { node = nodes.ToArray() },
                new edgescontent { edge = edges.ToArray() },
            };
            var gexf = new gexfcontent{graph = gc};
            using (var writer = File.Create(_opts.Out)) {
                new XmlSerializer(typeof(gexfcontent))
                    .Serialize(writer, gexf);
            }

            Console.Write("Done");
        }
    }

    public class JsonInput
    {
        [JsonProperty("clusters")]
        public IList<Cluster> Clusters { get; set; }
    }

    public class Cluster
    {
        [JsonProperty("nodes")]
        public IDictionary<string, Node> Nodes { get; set; }

        [JsonProperty("edges")]
        public IList<Edge> Edges { get; set; }
    }

    public class Edge
    {
        [JsonProperty("source")]
        public int Source { get; set; }

        [JsonProperty("target")]
        public int Target { get; set; }

        [JsonProperty("weight")]
        public float Weight { get; set; }
    }

    public class Node
    {
        private string _descr;
        public int Id { get; set; }

        [JsonProperty("code")]
        public string Code { get; set; }

        [JsonProperty("descr")]
        public string Descr {
            get => _descr ?? "unknown";
            set => _descr = value;
        }
    }
}
