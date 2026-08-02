using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using GlassSuite.ViewModels;

namespace GlassSuite.Views;

public partial class HomeCenterView : UserControl
{
    private readonly MainViewModel _vm;

    public HomeCenterView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
        Loaded += async (_, _) =>
        {
            await vm.LoadAnnouncementsAsync();
            await vm.CheckUpdateAsync();
            UpdateBadge.Visibility = vm.HasUpdate ? Visibility.Visible : Visibility.Collapsed;
            var ann = vm.Announcements.FirstOrDefault();
            AnnouncementBar.Text = ann == null ? "" : $"📢 {(ann.IsPromo ? "推广" : "公告")} · {ann.Title}：{ann.Content.Replace('\n', ' ')}";
            _vm.PropertyChanged += OnVmChanged;
        };
    }

    private void OnVmChanged(object? sender, PropertyChangedEventArgs e)
    {
        Dispatcher.Invoke(() =>
        {
            if (e.PropertyName == nameof(MainViewModel.HasUpdate))
            {
                UpdateBadge.Visibility = _vm.HasUpdate ? Visibility.Visible : Visibility.Collapsed;
            }
        });
    }
}
