using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class SectionGroup : SectionUI
    {
        public override string TabName { get; }

        private readonly List<SectionUI> _tabComponents;
        private readonly string[] _activeTabNames;

        private int _currentTabIndex = 0;
        private Vector2 _localTabScrollPosition = Vector2.zero;
        private static float ViewHeight => Screen.height * 0.45f; // // Maintain consistent view height

        private readonly List<SectionUI> _sections;
        public SectionGroup(string name, List<SectionUI> sections)
        {
            TabName = name;
            _tabComponents = sections.ToList();
            _activeTabNames = _tabComponents.Select(t => t.TabName).ToArray();
        }

        public override void DrawUI()
        {
            _currentTabIndex = GUILayout.Toolbar(_currentTabIndex, _activeTabNames, GUILayout.Height(60));
            var currentTab = _tabComponents[_currentTabIndex];

            GUILayout.BeginVertical(GUI.skin.box);

            _localTabScrollPosition = GUILayout.BeginScrollView(_localTabScrollPosition, GUILayout.MinHeight(ViewHeight), GUILayout.ExpandHeight(true));
            currentTab.DrawUI();
            GUILayout.EndScrollView();

            GUILayout.EndVertical();
        }

        public override void Update()
        {
            _tabComponents[_currentTabIndex].Update();
        }
    }
}
