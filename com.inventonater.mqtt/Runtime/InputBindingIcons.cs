using UnityEngine;

namespace Inventonater
{
    public static class InputBindingIcons
    {
        public static Texture2D BleMediaIcon = CreateSimpleTexture(Color.cyan);
        public static Texture2D BleMouseIcon = CreateSimpleTexture(Color.blue);
        public static Texture2D LocalDPadIcon = CreateSimpleTexture(Color.red);
        public static Texture2D LocalMediaIcon = CreateSimpleTexture(Color.white);

        public static Texture2D SpeakerIcon = CreateSimpleTexture(Color.blue);
        public static Texture2D LightsIcon = CreateSimpleTexture(Color.yellow);
        public static Texture2D ChromecastIcon = CreateSimpleTexture(Color.green);
        public static Texture2D ShellIcon = CreateSimpleTexture(Color.grey);

        public static Texture2D CreateSimpleTexture(Color color)
        {
            Texture2D texture = new Texture2D(64, 64);
            Color[] colors = new Color[64 * 64];

            for (int i = 0; i < colors.Length; i++) colors[i] = color;

            texture.SetPixels(colors);
            texture.Apply();
            return texture;
        }

        public static Texture2D Get(RemoteMapping mapping)
        {
            if(mapping.name == BleMediaIcon.name) return BleMediaIcon;
            if(mapping.name == BleMouseIcon.name) return BleMouseIcon;
            if(mapping.name == LocalDPadIcon.name) return LocalDPadIcon;
            if(mapping.name == LocalMediaIcon.name) return LocalMediaIcon;
            if(mapping.name == SpeakerIcon.name) return SpeakerIcon;
            if(mapping.name == LightsIcon.name) return LightsIcon;
            if(mapping.name == ChromecastIcon.name) return ChromecastIcon;
            if(mapping.name == ShellIcon.name) return ShellIcon;
            return CreateSimpleTexture(Color.magenta);
        }
    }
}
