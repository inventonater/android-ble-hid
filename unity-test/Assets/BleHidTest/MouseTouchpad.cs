using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System;

/// <summary>
/// Provides touchpad functionality for mouse control.
/// Detects touch/drag input within a designated screen area and converts it to mouse movement.
/// </summary>
public class MouseTouchpad : MonoBehaviour, IPointerDownHandler, IPointerUpHandler, IDragHandler
{
    [Header("Touchpad Settings")]
    [SerializeField] private float sensitivity = 1.0f;
    [SerializeField] private RectTransform touchpadArea;
    [SerializeField] private Image touchpadBackground;
    [SerializeField] private Color normalColor = new Color(0.8f, 0.8f, 0.8f, 0.5f);
    [SerializeField] private Color activeColor = new Color(0.6f, 0.6f, 0.6f, 0.7f);
    
    // Event fired when mouse movement occurs
    public event Action<Vector2> OnMouseMove;
    
    // Internal state
    private bool _isActive = false;
    private Vector2 _lastPosition;
    
    private void Awake()
    {
        // If no touchpad area is explicitly assigned, use this object's RectTransform
        if (touchpadArea == null)
        {
            touchpadArea = GetComponent<RectTransform>();
        }
        
        // If no background image is assigned, try to get one from this GameObject
        if (touchpadBackground == null)
        {
            touchpadBackground = GetComponent<Image>();
        }
        
        if (touchpadBackground != null)
        {
            touchpadBackground.color = normalColor;
        }
    }
    
    /// <summary>
    /// Called when pointer is pressed on the touchpad
    /// </summary>
    public void OnPointerDown(PointerEventData eventData)
    {
        if (RectTransformUtility.RectangleContainsScreenPoint(touchpadArea, eventData.position))
        {
            _isActive = true;
            _lastPosition = eventData.position;
            
            if (touchpadBackground != null)
            {
                touchpadBackground.color = activeColor;
            }
        }
    }
    
    /// <summary>
    /// Called when pointer is released from the touchpad
    /// </summary>
    public void OnPointerUp(PointerEventData eventData)
    {
        _isActive = false;
        
        if (touchpadBackground != null)
        {
            touchpadBackground.color = normalColor;
        }
    }
    
    /// <summary>
    /// Called when pointer is dragged on the touchpad
    /// </summary>
    public void OnDrag(PointerEventData eventData)
    {
        if (!_isActive) return;
        
        // Calculate movement delta
        Vector2 currentPosition = eventData.position;
        Vector2 delta = (currentPosition - _lastPosition) * sensitivity;
        
        // Only move if there's actually significant movement
        if (delta.sqrMagnitude > 0.01f)
        {
            // Fire the movement event
            OnMouseMove?.Invoke(delta);
        }
        
        _lastPosition = currentPosition;
    }
    
    /// <summary>
    /// Returns whether the touchpad is currently active (being touched)
    /// </summary>
    public bool IsActive()
    {
        return _isActive;
    }
    
    /// <summary>
    /// Sets the sensitivity of the touchpad
    /// </summary>
    public void SetSensitivity(float newSensitivity)
    {
        sensitivity = Mathf.Max(0.1f, newSensitivity);
    }
}
