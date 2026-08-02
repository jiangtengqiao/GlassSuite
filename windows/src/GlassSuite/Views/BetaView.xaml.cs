using System.Windows;
using System.Windows.Controls;
using GlassSuite.ViewModels;

namespace GlassSuite.Views;

public partial class BetaView : UserControl
{
    private readonly MainViewModel _vm;

    public BetaView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        Loaded += async (_, _) =>
        {
            var req = await vm.GetBetaRequirementsAsync();
            ReqIntro.Text = req.Intro;
            ReqList.ItemsSource = req.Items;
            ReqNote.Text = $"评分阈值：≥ {req.ScoreThreshold} 分通过。{req.Note}";
            if (vm.BetaAccess)
            {
                ActivateMsg.Text = $"已激活尝鲜权限（可接收 Beta 版推送）· 当前尝鲜码：{vm.BetaKey}";
                ActivateMsg.Foreground = System.Windows.Media.Brushes.SeaGreen;
            }
        };
    }

    private async void Apply_Click(object sender, RoutedEventArgs e)
    {
        var purpose = (PurposeBox.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "开发者";
        var result = await _vm.ApplyBetaAsync(EmailBox.Text.Trim(), NameBox.Text.Trim(), purpose,
            ReasonBox.Text.Trim(), DeviceBox.Text.Trim());
        ResultPanel.Visibility = Visibility.Visible;
        if (result.Status == "approved")
        {
            ResultStatus.Text = "✅ 申请通过！";
            ResultStatus.Foreground = System.Windows.Media.Brushes.SeaGreen;
            ResultScore.Text = $"评分：{result.Score} / 90";
            ResultKey.Text = $"你的尝鲜码（{result.Key.Length} 位）：{result.Key}";
            ResultReasons.ItemsSource = null;
        }
        else
        {
            ResultStatus.Text = "❌ 申请未通过（可修改后重新申请）";
            ResultStatus.Foreground = System.Windows.Media.Brushes.Firebrick;
            ResultScore.Text = $"评分：{result.Score} / 90（阈值 60）";
            ResultKey.Text = "";
            ResultReasons.ItemsSource = result.Reasons;
        }
        ResultMsg.Text = result.Message;
    }

    private async void Verify_Click(object sender, RoutedEventArgs e)
    {
        var ok = await _vm.VerifyBetaAsync(KeyBox.Text.Trim());
        ActivateMsg.Text = ok
            ? $"✅ 尝鲜码有效，已激活！可接收 Beta 版推送（当前 v1.1.0-beta.1）"
            : "❌ 尝鲜码无效或已被停用，请核对后重试";
        ActivateMsg.Foreground = ok ? System.Windows.Media.Brushes.SeaGreen : System.Windows.Media.Brushes.Firebrick;
    }
}
