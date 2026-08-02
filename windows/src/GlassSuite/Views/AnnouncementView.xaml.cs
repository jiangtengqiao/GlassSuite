using System.Windows;
using System.Windows.Controls;
using GlassSuite.Services;

namespace GlassSuite.Views;

public partial class AnnouncementView : UserControl
{
    private readonly ViewModels.MainViewModel _vm;

    public AnnouncementView(ViewModels.MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
        Loaded += async (_, _) =>
        {
            await vm.LoadAnnouncementsAsync();
        };
    }
}
