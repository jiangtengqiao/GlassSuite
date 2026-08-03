using System.Windows;
using GlassSuite.Services;

namespace GlassSuite;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        ErrorReporter.Init();
    }
}
