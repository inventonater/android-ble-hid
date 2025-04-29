using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class TabGroup : SectionUI
    {
        public override string TabName { get; }

        private List<SectionUI> _tabComponents;
        private string[] _activeTabNames;

        private int _currentTabIndex = 0;
        private Vector2 _localTabScrollPosition = Vector2.zero;
        private static float ViewHeight => Screen.height * 0.45f; // // Maintain consistent view height

        private readonly List<SectionUI> _sections;
        public TabGroup(string name, List<SectionUI> sections)
        {
            TabName = name;
            UpdateTabs(sections);
        }

        public void SetCurrentTab(string tabName)
        {
            var index = _activeTabNames.ToList().IndexOf(tabName);
            if (index != -1) _currentTabIndex = index;
        }

        public void AddTab(SectionUI section)
        {
            _tabComponents.Add(section);
            UpdateTabs(_tabComponents);
        }

        public void RemoveTab(SectionUI section)
        {
            _tabComponents.Remove(section);
            UpdateTabs(_tabComponents);
        }

        private void UpdateTabs(List<SectionUI> sections)
        {
            _tabComponents = sections.ToList();
            _activeTabNames = _tabComponents.Select(t => t.TabName).ToArray();
            _currentTabIndex = Mathf.Clamp(_currentTabIndex, 0, _activeTabNames.Length - 1);
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
