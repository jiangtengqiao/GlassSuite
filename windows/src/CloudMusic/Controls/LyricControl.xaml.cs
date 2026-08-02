using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using CloudMusic.Services;

namespace CloudMusic.Controls;

public partial class LyricControl : UserControl
{
    private int _currentIndex = -1;
    private List<TextBlock> _blocks = new();

    public LyricControl()
    {
        InitializeComponent();
    }

    public void SetLyrics(IEnumerable<LrcLine> lines)
    {
        Items.ItemsSource = lines;
        Items.UpdateLayout();
        _blocks.Clear();
        for (int i = 0; i < Items.Items.Count; i++)
        {
            if (Items.ItemContainerGenerator.ContainerFromIndex(i) is ContentPresenter cp)
            {
                var tb = cp.FindVisualChild<TextBlock>();
                if (tb != null) _blocks.Add(tb);
            }
        }
        _currentIndex = -1;
        SetCurrentIndex(0);
    }

    public void SetCurrentIndex(int index)
    {
        if (index == _currentIndex) return;
        if (index >= 0 && index < _blocks.Count)
        {
            var prev = _currentIndex >= 0 && _currentIndex < _blocks.Count ? _blocks[_currentIndex] : null;
            if (prev != null)
            {
                prev.Foreground = new SolidColorBrush(Color.FromRgb(0x88, 0x88, 0x88));
                prev.FontWeight = FontWeights.Normal;
            }
            var cur = _blocks[index];
            cur.Foreground = new SolidColorBrush(Color.FromRgb(0xC6, 0x2F, 0x2F));
            cur.FontWeight = FontWeights.Bold;
            Scroller.ScrollToVerticalOffset(Math.Max(0,
                cur.TranslatePoint(new Point(0, 0), Scroller).Y - Scroller.ActualHeight / 2 + 20));
        }
        _currentIndex = index;
    }
}

internal static class VisualTreeHelperExt
{
    public static T? FindVisualChild<T>(this DependencyObject parent) where T : DependencyObject
    {
        for (int i = 0; i < VisualTreeHelper.GetChildrenCount(parent); i++)
        {
            var child = VisualTreeHelper.GetChild(parent, i);
            if (child is T t) return t;
            var found = child.FindVisualChild<T>();
            if (found != null) return found;
        }
        return null;
    }
}
