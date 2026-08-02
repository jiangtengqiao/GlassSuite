using System;
using System.ComponentModel;
using System.IO;
using System.Windows;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using CloudMusic.ViewModels;

namespace CloudMusic.Views;

public partial class LoginView : System.Windows.Controls.UserControl
{
    private readonly MainViewModel _vm;
    private DispatcherTimer? _cdTimer;

    public LoginView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        _vm.PropertyChanged += OnVmPropertyChanged;
        Loaded += (_, _) =>
        {
            _vm.StartQr();
            QrStatus.Text = _vm.QrStatus;
        };
        Unloaded += (_, _) =>
        {
            _vm.PropertyChanged -= OnVmPropertyChanged;
            _cdTimer?.Stop();
        };
    }

    private void OnVmPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        Dispatcher.Invoke(() =>
        {
            if (e.PropertyName == nameof(MainViewModel.QrImage))
            {
                SetQrImage(_vm.QrImage);
            }
            else if (e.PropertyName == nameof(MainViewModel.QrStatus))
            {
                QrStatus.Text = _vm.QrStatus;
            }
            else if (e.PropertyName == nameof(MainViewModel.SmsStatus))
            {
                SmsStatus.Text = _vm.SmsStatus;
            }
        });
    }

    private void SetQrImage(string base64)
    {
        try
        {
            if (string.IsNullOrEmpty(base64))
            {
                QrImage.Source = null;
                return;
            }
            var idx = base64.IndexOf(',');
            var b64 = idx >= 0 ? base64[(idx + 1)..] : base64;
            var bytes = Convert.FromBase64String(b64);
            using var ms = new MemoryStream(bytes);
            var bmp = new BitmapImage();
            bmp.BeginInit();
            bmp.CacheOption = BitmapCacheOption.OnLoad;
            bmp.StreamSource = ms;
            bmp.EndInit();
            QrImage.Source = bmp;
        }
        catch
        {
            QrImage.Source = null;
        }
    }

    private void SendBtn_Click(object sender, RoutedEventArgs e)
    {
        if (PhoneBox.Text.Length != 11)
        {
            SmsStatus.Text = "请输入 11 位手机号";
            return;
        }
        _vm.Phone = PhoneBox.Text;
        _ = _vm.SendCodeAsync();
        SendBtn.IsEnabled = false;
        var left = 60;
        _cdTimer?.Stop();
        _cdTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
        _cdTimer.Tick += (_, _) =>
        {
            left--;
            SendBtn.Content = $"{left}s";
            if (left <= 0)
            {
                _cdTimer.Stop();
                SendBtn.IsEnabled = true;
                SendBtn.Content = "发送验证码";
            }
        };
        _cdTimer.Start();
    }

    private void LoginBtn_Click(object sender, RoutedEventArgs e)
    {
        if (PhoneBox.Text.Length != 11)
        {
            SmsStatus.Text = "请输入 11 位手机号";
            return;
        }
        if (CodeBox.Text.Length < 4)
        {
            SmsStatus.Text = "请输入验证码";
            return;
        }
        _vm.Phone = PhoneBox.Text;
        _vm.SmsCode = CodeBox.Text;
        _ = _vm.SmsLoginAsync();
    }
}
