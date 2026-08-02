using System.Diagnostics;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using GlassSuite.Services;
using GlassSuite.ViewModels;

namespace GlassSuite.Views;

public partial class GitHubView : UserControl
{
    private readonly MainViewModel _vm;

    public GitHubView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
        Loaded += async (_, _) =>
        {
            await vm.LoadGitHubAsync();
            StatusText.Text = vm.GitHubStatus;
            _vm.PropertyChanged += (_, e) =>
            {
                if (e.PropertyName == nameof(MainViewModel.GitHubStatus))
                {
                    Dispatcher.Invoke(() => StatusText.Text = _vm.GitHubStatus);
                }
            };
        };
    }

    private async void Search_Click(object sender, RoutedEventArgs e)
    {
        await _vm.LoadGitHubAsync();
    }

    private void RepoList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (RepoList.SelectedItem is GitHubRepo repo)
        {
            try
            {
                Process.Start(new ProcessStartInfo(repo.HtmlUrl) { UseShellExecute = true });
            }
            catch
            {
            }
        }
    }
}
