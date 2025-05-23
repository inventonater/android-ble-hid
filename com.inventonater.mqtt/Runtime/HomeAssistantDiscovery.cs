using System;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using Cysharp.Threading.Tasks;
using UnityEngine;

namespace Inventonater
{
    public static class HomeAssistantDiscovery
    {
        private const string DefaultHostname = "homeassistant.local";

        public static async UniTask<string> DiscoverHomeAssistantIpAsync(string hostname = null, CancellationToken cancellationToken = default)
        {
            hostname ??= DefaultHostname;

            try
            {
                var hostEntry = await UniTask.RunOnThreadPool(() => ResolveHostname(hostname), cancellationToken: cancellationToken);
                
                if (hostEntry == null || hostEntry.AddressList.Length == 0) return null;
                
                // Find first IPv4 address
                var ipv4Address = hostEntry.AddressList.FirstOrDefault(addr => addr.AddressFamily == AddressFamily.InterNetwork)?.ToString();
                
                if (!string.IsNullOrEmpty(ipv4Address)) Debug.Log($"Discovered HomeAssistant at {ipv4Address}");
                
                return ipv4Address;
            }
            catch (OperationCanceledException)
            {
                Debug.Log("HomeAssistant discovery was canceled");
                throw;
            }
            catch (Exception ex)
            {
                Debug.LogError($"Error during HomeAssistant discovery: {ex.Message}");
                return null;
            }
        }

        private static IPHostEntry ResolveHostname(string hostname)
        {
            try
            {
                return Dns.GetHostEntry(hostname);
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"Failed to resolve {hostname}: {ex.Message}");
                return null;
            }
        }
    }
}
