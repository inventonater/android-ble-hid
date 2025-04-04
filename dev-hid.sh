#!/bin/bash

# =============================================================================
# dev-hid.sh - Main developer tool launcher for Android BLE HID project
# =============================================================================
# This script provides a unified interface to access all available developer tools

# Source common functions
source "$(dirname "$0")/developer-tools/common.sh"

# Make sure we're in the project root
navigate_to_project_root

# Ensure all scripts are executable
chmod +x developer-tools/*.sh

clear
echo -e "${BOLD}=====================================================${NC}"
echo -e "${BOLD}     Android BLE HID Developer Tools Launcher        ${NC}"
echo -e "${BOLD}=====================================================${NC}"
echo ""
echo "Please select a tool:"
echo ""
echo -e "${BLUE}Build Tools:${NC}"
echo "  1) Build All Components"
echo "  2) Build Android App Only"
echo "  3) Build Unity Plugin Only"
echo ""
echo -e "${GREEN}Application Tools:${NC}"
echo "  4) Run Android App (Build, Install, Launch)"
echo ""
echo -e "${YELLOW}Diagnostic Tools:${NC}"
echo "  5) Check Device Compatibility"
echo "  6) Debug Connection Issues"
echo "  7) Test Mouse Functionality"
echo "  8) Test Debug Features"
echo ""
echo -e "${RED}Advanced Tools:${NC}"
echo "  9) Clean Project"
echo " 10) Show Dependencies"
echo ""
echo "  0) Exit"
echo ""

read -p "Enter selection [0-10]: " SELECTION

case $SELECTION in
    1)
        echo -e "${BLUE}Building all components...${NC}"
        ./developer-tools/build-all.sh
        ;;
    2)
        echo -e "${BLUE}Building Android app...${NC}"
        ./developer-tools/build-app.sh
        ;;
    3)
        echo -e "${BLUE}Building Unity plugin...${NC}"
        ./developer-tools/build-unity.sh
        ;;
    4)
        echo -e "${GREEN}Running Android app...${NC}"
        ./developer-tools/run-app.sh
        ;;
    5)
        echo -e "${YELLOW}Checking device compatibility...${NC}"
        ./developer-tools/check-device.sh
        ;;
    6)
        echo -e "${YELLOW}Debugging connection issues...${NC}"
        echo "Would you like to run enhanced diagnostics? (y/n)"
        read ENHANCED
        if [[ $ENHANCED == "y" || $ENHANCED == "Y" ]]; then
            ./developer-tools/debug-connection.sh --enhanced
        else
            ./developer-tools/debug-connection.sh
        fi
        ;;
    7)
        echo -e "${YELLOW}Testing mouse functionality...${NC}"
        ./developer-tools/test-mouse.sh
        ;;
    8)
        echo -e "${YELLOW}Testing debug features...${NC}"
        ./developer-tools/test-debug-features.sh
        ;;
    9)
        echo -e "${RED}Cleaning project...${NC}"
        ./gradlew clean
        echo "Project cleaned."
        ;;
    10)
        echo -e "${RED}Showing dependencies...${NC}"
        ./gradlew showDependencies
        ;;
    0)
        echo "Exiting."
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid selection.${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}Tool execution complete.${NC}"
echo "Run './dev-hid.sh' to return to the main menu."
